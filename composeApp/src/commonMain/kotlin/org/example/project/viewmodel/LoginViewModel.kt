package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.CaptchaResponse
import org.example.project.data.PanelConfig
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService
import org.example.project.network.rsaEncryptOaepSha512

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

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        _config.value = cfg
        service?.close()
        service = PanelApiService(cfg)
        _is2fa.value = false
        viewModelScope.launch {
            // Session 模式且有安全入口时，先初始化入口获取验证 Cookie
            if (cfg.entrance.isNotEmpty() && cfg.authMode == "session") {
                service?.initEntrance()
            }
            loadPublicKey()
            checkCaptcha()
        }
    }

    private suspend fun loadPublicKey() {
        service?.getPublicKey()
            ?.onSuccess { rsaPublicKey = it }
            ?.onFailure { e ->
                _loginStatus.value = LoginStatus.Error("无法获取公钥: ${e.message}")
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
            _loginStatus.value = LoginStatus.Error("用户名和密码不能为空")
            return
        }
        viewModelScope.launch {
            check2fa(uname)
            if (_is2fa.value && passCode.value.trim().isEmpty()) {
                _loginStatus.value = LoginStatus.Error("该用户已开启两步验证，请输入 2FA 验证码")
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
                val encUser = rsaEncryptOaepSha512(rsaPublicKey, uname.toByteArray(Charsets.UTF_8))
                val encPwd = rsaEncryptOaepSha512(rsaPublicKey, pwd.toByteArray(Charsets.UTF_8))
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
                    _loginStatus.value = LoginStatus.Error(e.message ?: "登录失败")
                    // 登录失败后刷新验证码
                    checkCaptcha()
                }
            } catch (e: Exception) {
                _loginStatus.value = LoginStatus.Error(e.message ?: "加密失败")
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
