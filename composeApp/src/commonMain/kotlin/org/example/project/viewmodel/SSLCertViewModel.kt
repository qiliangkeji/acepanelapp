package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.CertListItem
import org.example.project.data.CreateCertRequest
import org.example.project.data.PanelRepository
import org.example.project.data.UploadCertRequest
import org.example.project.network.PanelApiService

class SSLCertViewModel : ViewModel() {

    private val _certs = MutableStateFlow<List<CertListItem>>(emptyList())
    val certs: StateFlow<List<CertListItem>> = _certs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionId = MutableStateFlow<Long?>(null)
    val actionId: StateFlow<Long?> = _actionId

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    // 创建/上传证书对话框
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _showUploadDialog = MutableStateFlow(false)
    val showUploadDialog: StateFlow<Boolean> = _showUploadDialog

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch { load() }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getCerts(page = 1, limit = 50)
                ?.onSuccess { _certs.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreate() { _actionError.value = null; _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }
    fun showUpload() { _actionError.value = null; _showUploadDialog.value = true }
    fun hideUpload() { _showUploadDialog.value = false }

    fun createCert(type: String, domains: List<String>, autoRenewal: Boolean, accountId: Long, dnsId: Long) {
        viewModelScope.launch {
            _actionError.value = null
            val req = CreateCertRequest(
                type = type,
                domains = domains,
                auto_renewal = autoRenewal,
                account_id = accountId,
                dns_id = dnsId
            )
            service?.createCert(req)
                ?.onSuccess { _showCreateDialog.value = false; load() }
                ?.onFailure { _actionError.value = "创建失败: ${it.message}" }
        }
    }

    fun uploadCert(cert: String, key: String) {
        viewModelScope.launch {
            _actionError.value = null
            val req = UploadCertRequest(cert = cert, key = key)
            service?.uploadCert(req)
                ?.onSuccess { _showUploadDialog.value = false; load() }
                ?.onFailure { _actionError.value = "上传失败: ${it.message}" }
        }
    }

    fun obtainAuto(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.obtainAutoSigned(id)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "ACME签发失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun obtainSelf(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.obtainSelfSigned(id)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "自签名失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun renew(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.renewCert(id)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "续期失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteCert(id)
                ?.onSuccess { _certs.value = _certs.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
