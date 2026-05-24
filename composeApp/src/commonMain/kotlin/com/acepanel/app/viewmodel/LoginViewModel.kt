package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CaptchaResponse
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.feedback.FeedbackCenter
import com.acepanel.app.network.PanelApiService
import com.acepanel.app.network.rsaEncryptOaepSha512

class LoginViewModel : ViewModel() {

    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val passCode = MutableStateFlow("")
    val captchaCode = MutableStateFlow("")

    private val _config = MutableStateFlow<PanelConfig?>(null)
    val config: StateFlow<PanelConfig?> = _config

    private val _captcha = MutableStateFlow<CaptchaResponse?>(null)
    val captcha: StateFlow<CaptchaResponse?> = _captcha

    private val _is2fa = MutableStateFlow(false)
    val is2fa: StateFlow<Boolean> = _is2fa

    private val _loginStatus = MutableStateFlow<LoginStatus>(LoginStatus.Idle)
    val loginStatus: StateFlow<LoginStatus> = _loginStatus

    private var service: PanelApiService? = null
    private var rsaPublicKey: String = ""
    private var check2faJob: Job? = null

    fun init(
        panelId: String,
        initialUsername: String = "",
        initialPassword: String = "",
        autoLogin: Boolean = false,
        onAutoLoginSuccess: () -> Unit = {}
    ) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        _config.value = cfg
        service?.close()
        service = PanelApiService(cfg)
        rsaPublicKey = ""
        _captcha.value = null
        _is2fa.value = false
        username.value = initialUsername.ifBlank { cfg.sessionUsername }
        if (initialPassword.isNotBlank()) {
            password.value = initialPassword
        }
        viewModelScope.launch {
            // Session 模式先初始化入口获取 verify_entrance Cookie；入口为 / 时也需要建立同一个 session
            if (cfg.authMode == "session") {
                service?.initEntrance()?.onFailure { e ->
                    val message = e.message ?: "安全入口验证失败"
                    _loginStatus.value = LoginStatus.Error(message)
                    FeedbackCenter.error("登录准备失败", message)
                    return@launch
                }
            }
            loadPublicKey()
            checkCaptcha()
            if (
                autoLogin &&
                username.value.trim().isNotEmpty() &&
                password.value.isNotEmpty() &&
                rsaPublicKey.isNotEmpty()
            ) {
                if (_captcha.value?.required == true) {
                    val message = "需要输入图形验证码后再登录"
                    _loginStatus.value = LoginStatus.Error(message)
                    FeedbackCenter.warning("需要图形验证码", message)
                } else {
                    login(onAutoLoginSuccess)
                }
            }
        }
    }

    private suspend fun loadPublicKey() {
        service?.getPublicKey()
            ?.onSuccess { rsaPublicKey = it }
            ?.onFailure { e ->
                val message = "无法获取公钥: ${e.message}"
                _loginStatus.value = LoginStatus.Error(message)
                FeedbackCenter.error("登录准备失败", message)
            }
    }

    private suspend fun checkCaptcha() {
        service?.getCaptcha()?.onSuccess { _captcha.value = it }
    }

    private suspend fun check2fa(uname: String) {
        val normalized = uname.trim()
        if (normalized.isBlank()) {
            _is2fa.value = false
            return
        }
        val svc = service ?: run {
            _is2fa.value = false
            return
        }
        svc.isTwoFaEnabled(normalized)
            .onSuccess { enabled -> _is2fa.value = enabled }
            .onFailure { _is2fa.value = false }
    }

    fun onUsernameChanged(value: String) {
        username.value = value
        check2faJob?.cancel()
        check2faJob = viewModelScope.launch {
            delay(250)
            check2fa(value)
        }
    }

    fun login(onSuccess: () -> Unit) {
        val uname = username.value.trim()
        val pwd = password.value
        if (uname.isEmpty() || pwd.isEmpty()) {
            val message = "用户名和密码不能为空"
            _loginStatus.value = LoginStatus.Error(message)
            FeedbackCenter.warning("无法登录", message)
            return
        }
        viewModelScope.launch {
            check2fa(uname)
            if (_is2fa.value && passCode.value.trim().isEmpty()) {
                val message = "该用户已开启两步验证，请输入 2FA 验证码"
                _loginStatus.value = LoginStatus.Error(message)
                FeedbackCenter.warning("需要两步验证", message)
                return@launch
            }
            // 若公钥尚未加载，先重试获取
            if (rsaPublicKey.isEmpty()) {
                _loginStatus.value = LoginStatus.Loading
                loadPublicKey()
            }
            if (rsaPublicKey.isEmpty()) {
                // loadPublicKey() 已写入具体错误，不再覆盖
                return@launch
            }
            _loginStatus.value = LoginStatus.Loading
            try {
                val encUser = rsaEncryptOaepSha512(rsaPublicKey, uname.encodeToByteArray())
                val encPwd = rsaEncryptOaepSha512(rsaPublicKey, pwd.encodeToByteArray())
                service?.login(
                    encryptedUsername = encUser,
                    encryptedPassword = encPwd,
                    passCode = passCode.value.trim(),
                    captchaCode = captchaCode.value.trim()
                )?.onSuccess {
                    _loginStatus.value = LoginStatus.Success
                    onSuccess()
                }?.onFailure { e ->
                    if ((e.message ?: "").contains("2FA", ignoreCase = true)) {
                        _is2fa.value = true
                    }
                    val message = e.message ?: "登录失败"
                    _loginStatus.value = LoginStatus.Error(message)
                    // 登录失败后刷新验证码
                    checkCaptcha()
                }
            } catch (e: Exception) {
                val message = e.message ?: "加密失败"
                _loginStatus.value = LoginStatus.Error(message)
                FeedbackCenter.error("登录失败", message)
            }
        }
    }

    sealed class LoginStatus {
        object Idle : LoginStatus()
        object Loading : LoginStatus()
        object Success : LoginStatus()
        data class Error(val message: String) : LoginStatus()
    }

    override fun onCleared() {
        check2faJob?.cancel()
        service?.close()
        super.onCleared()
    }
}
