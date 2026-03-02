package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.CreateFirewallRuleRequest
import org.example.project.data.CreateForwardRuleRequest
import org.example.project.data.CreateIpRuleRequest
import org.example.project.data.FirewallRuleApiItem
import org.example.project.data.ForwardRuleApiItem
import org.example.project.data.IpRuleApiItem
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService

class FirewallViewModel : ViewModel() {

    private val _status = MutableStateFlow(false)
    val status: StateFlow<Boolean> = _status

    private val _rules = MutableStateFlow<List<FirewallRuleApiItem>>(emptyList())
    val rules: StateFlow<List<FirewallRuleApiItem>> = _rules

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    private val _ipRules = MutableStateFlow<List<IpRuleApiItem>>(emptyList())
    val ipRules: StateFlow<List<IpRuleApiItem>> = _ipRules

    private val _forwardRules = MutableStateFlow<List<ForwardRuleApiItem>>(emptyList())
    val forwardRules: StateFlow<List<ForwardRuleApiItem>> = _forwardRules

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private var service: PanelApiService? = null
    private var loadJob: Job? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val svc = service ?: return@launch
            _isLoading.value = true
            _error.value = null
            try {
                coroutineScope {
                    launch {
                        svc.getFirewallStatus().onSuccess { _status.value = it }
                    }
                    launch {
                        svc.getFirewallRules(limit = 100).onSuccess {
                            _rules.value = it.items
                            _total.value = it.total
                        }.onFailure { _error.value = it.message }
                    }
                    launch {
                        svc.getIpRules(limit = 100).onSuccess { _ipRules.value = it.items }
                    }
                    launch {
                        svc.getForwardRules(limit = 100).onSuccess { _forwardRules.value = it.items }
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleStatus() {
        viewModelScope.launch {
            val newStatus = !_status.value
            service?.setFirewallStatus(newStatus)?.onSuccess {
                _status.value = newStatus
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    fun deleteRule(rule: FirewallRuleApiItem) {
        viewModelScope.launch {
            val req = CreateFirewallRuleRequest(
                type = rule.type, family = rule.family,
                port_start = rule.port_start, port_end = rule.port_end,
                protocol = rule.protocol, address = rule.address,
                strategy = rule.strategy, direction = rule.direction
            )
            service?.deleteFirewallRule(req)?.onSuccess { load() }
                ?.onFailure { _error.value = it.message }
        }
    }

    fun addIpRule(family: String, protocol: String, address: String, strategy: String, direction: String) {
        viewModelScope.launch {
            _actionError.value = null
            val req = CreateIpRuleRequest(family, protocol, address, strategy, direction)
            service?.addIpRule(req)?.onSuccess {
                _ipRules.value = (_ipRules.value + IpRuleApiItem(family, protocol, address, strategy, direction))
            }?.onFailure { _actionError.value = it.message }
        }
    }

    fun deleteIpRule(rule: IpRuleApiItem) {
        viewModelScope.launch {
            val req = CreateIpRuleRequest(rule.family, rule.protocol, rule.address, rule.strategy, rule.direction)
            service?.deleteIpRule(req)?.onSuccess {
                _ipRules.value = _ipRules.value.filter { it.address != rule.address || it.protocol != rule.protocol }
            }?.onFailure { _actionError.value = it.message }
        }
    }

    fun addRule(family: String, portStart: Int, portEnd: Int, protocol: String,
                address: String, strategy: String, direction: String) {
        viewModelScope.launch {
            _actionError.value = null
            val req = CreateFirewallRuleRequest(
                family = family, port_start = portStart, port_end = portEnd,
                protocol = protocol, address = address, strategy = strategy, direction = direction
            )
            service?.addFirewallRule(req)?.onSuccess { load() }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun addForwardRule(protocol: String, port: Int, targetIp: String, targetPort: Int) {
        viewModelScope.launch {
            _actionError.value = null
            val req = CreateForwardRuleRequest(protocol, port, targetIp, targetPort)
            service?.addForwardRule(req)?.onSuccess {
                _forwardRules.value = (_forwardRules.value + ForwardRuleApiItem(protocol, port, targetIp, targetPort))
            }?.onFailure { _actionError.value = it.message }
        }
    }

    fun deleteForwardRule(rule: ForwardRuleApiItem) {
        viewModelScope.launch {
            val req = CreateForwardRuleRequest(rule.protocol, rule.port, rule.target_ip, rule.target_port)
            service?.deleteForwardRule(req)?.onSuccess {
                _forwardRules.value = _forwardRules.value.filter { it.port != rule.port || it.protocol != rule.protocol }
            }?.onFailure { _actionError.value = it.message }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        service?.close()
        super.onCleared()
    }
}
