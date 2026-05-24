package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.AcmeAccountItem
import com.acepanel.app.data.CreateAcmeAccountRequest
import com.acepanel.app.data.CreateDnsProviderRequest
import com.acepanel.app.data.DnsParamData
import com.acepanel.app.data.DnsProviderItem
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class CertConfigViewModel : ViewModel() {

    // ACME 账号
    private val _acmeAccounts = MutableStateFlow<List<AcmeAccountItem>>(emptyList())
    val acmeAccounts: StateFlow<List<AcmeAccountItem>> = _acmeAccounts

    private val _isAcmeLoading = MutableStateFlow(false)
    val isAcmeLoading: StateFlow<Boolean> = _isAcmeLoading

    // DNS 凭据
    private val _dnsProviders = MutableStateFlow<List<DnsProviderItem>>(emptyList())
    val dnsProviders: StateFlow<List<DnsProviderItem>> = _dnsProviders

    private val _isDnsLoading = MutableStateFlow(false)
    val isDnsLoading: StateFlow<Boolean> = _isDnsLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch {
            loadAcme()
            loadDns()
        }
    }

    fun loadAcme() {
        viewModelScope.launch {
            _isAcmeLoading.value = true
            _error.value = null
            service?.getAcmeAccounts()
                ?.onSuccess { _acmeAccounts.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isAcmeLoading.value = false
        }
    }

    fun createAcmeAccount(
        ca: String, email: String, keyType: String,
        kid: String = "", hmacEncoded: String = ""
    ) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createAcmeAccount(
                CreateAcmeAccountRequest(ca, email, keyType, kid, hmacEncoded)
            )
                ?.onSuccess { loadAcme() }
                ?.onFailure { _actionError.value = "创建失败: ${it.message}" }
        }
    }

    fun updateAcmeAccount(
        id: Long, ca: String, email: String, keyType: String,
        kid: String = "", hmacEncoded: String = ""
    ) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updateAcmeAccount(id, CreateAcmeAccountRequest(ca, email, keyType, kid, hmacEncoded))
                ?.onSuccess { loadAcme() }
                ?.onFailure { _actionError.value = "更新失败: ${it.message}" }
        }
    }

    fun deleteAcmeAccount(id: Long) {
        viewModelScope.launch {
            _actionError.value = null
            service?.deleteAcmeAccount(id)
                ?.onSuccess { _acmeAccounts.value = _acmeAccounts.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
        }
    }

    fun loadDns() {
        viewModelScope.launch {
            _isDnsLoading.value = true
            _error.value = null
            service?.getDnsProviders()
                ?.onSuccess { _dnsProviders.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isDnsLoading.value = false
        }
    }

    fun createDnsProvider(type: String, name: String, ak: String, sk: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createDnsProvider(
                CreateDnsProviderRequest(type, name, DnsParamData(ak, sk))
            )
                ?.onSuccess { loadDns() }
                ?.onFailure { _actionError.value = "创建失败: ${it.message}" }
        }
    }

    fun updateDnsProvider(id: Long, type: String, name: String, ak: String, sk: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updateDnsProvider(id, CreateDnsProviderRequest(type, name, DnsParamData(ak, sk)))
                ?.onSuccess { loadDns() }
                ?.onFailure { _actionError.value = "更新失败: ${it.message}" }
        }
    }

    fun deleteDnsProvider(id: Long) {
        viewModelScope.launch {
            _actionError.value = null
            service?.deleteDnsProvider(id)
                ?.onSuccess { _dnsProviders.value = _dnsProviders.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
        }
    }

    fun clearError() {
        _actionError.value = null
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
