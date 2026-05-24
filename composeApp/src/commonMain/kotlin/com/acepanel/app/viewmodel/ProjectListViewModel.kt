package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateProjectRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.ProjectListItem
import com.acepanel.app.data.UpdateProjectRequest
import com.acepanel.app.network.PanelApiService

class ProjectListViewModel : ViewModel() {

    private val _projects = MutableStateFlow<List<ProjectListItem>>(emptyList())
    val projects: StateFlow<List<ProjectListItem>> = _projects

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    val typeFilter = MutableStateFlow("all")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actingName = MutableStateFlow<String?>(null)
    val actingName: StateFlow<String?> = _actingName

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        load()
    }

    fun loadByType(type: String) {
        typeFilter.value = type
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getProjects(type = typeFilter.value, page = 1, limit = 50)
                ?.onSuccess { resp ->
                    _projects.value = resp.items
                    _total.value = resp.total
                }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun start(name: String) {
        viewModelScope.launch {
            _actingName.value = name
            _actionError.value = null
            service?.systemctlStart(name)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "启动失败: ${it.message}" }
            _actingName.value = null
        }
    }

    fun stop(name: String) {
        viewModelScope.launch {
            _actingName.value = name
            _actionError.value = null
            service?.systemctlStop(name)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "停止失败: ${it.message}" }
            _actingName.value = null
        }
    }

    fun restart(name: String) {
        viewModelScope.launch {
            _actingName.value = name
            _actionError.value = null
            service?.systemctlRestart(name)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "重启失败: ${it.message}" }
            _actingName.value = null
        }
    }

    fun toggleEnable(project: ProjectListItem) {
        viewModelScope.launch {
            _actingName.value = project.name
            if (project.enabled) {
                service?.systemctlDisable(project.name)
                    ?.onSuccess { load() }
                    ?.onFailure { _actionError.value = "操作失败: ${it.message}" }
            } else {
                service?.systemctlEnable(project.name)
                    ?.onSuccess { load() }
                    ?.onFailure { _actionError.value = "操作失败: ${it.message}" }
            }
            _actingName.value = null
        }
    }

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    private val _editingProject = MutableStateFlow<ProjectListItem?>(null)
    val editingProject: StateFlow<ProjectListItem?> = _editingProject

    private val _isProjectLoading = MutableStateFlow(false)
    val isProjectLoading: StateFlow<Boolean> = _isProjectLoading

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    fun showCreate() { _showCreateDialog.value = true; _actionError.value = null }
    fun hideCreate() { _showCreateDialog.value = false; _actionError.value = null }

    fun createProject(name: String, type: String, description: String,
                      rootDir: String, execStart: String, user: String) {
        viewModelScope.launch {
            _isCreating.value = true
            _actionError.value = null
            service?.createProject(CreateProjectRequest(
                name = name, type = type, description = description,
                root_dir = rootDir, exec_start = execStart, user = user
            ))?.onSuccess {
                _showCreateDialog.value = false
                load()
            }?.onFailure { _actionError.value = it.message }
            _isCreating.value = false
        }
    }

    fun showEdit(project: ProjectListItem) {
        viewModelScope.launch {
            _actionError.value = null
            _editingProject.value = project
            _isProjectLoading.value = true
            service?.getProject(project.id)
                ?.onSuccess { _editingProject.value = it }
                ?.onFailure { _actionError.value = "加载项目详情失败: ${it.message}" }
            _isProjectLoading.value = false
        }
    }

    fun hideEdit() {
        _editingProject.value = null
        _actionError.value = null
    }

    fun updateProject(req: UpdateProjectRequest) {
        viewModelScope.launch {
            _isUpdating.value = true
            _actionError.value = null
            service?.updateProject(req.id, req)
                ?.onSuccess {
                    _editingProject.value = null
                    load()
                }
                ?.onFailure { _actionError.value = "保存失败: ${it.message}" }
            _isUpdating.value = false
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val project = _projects.value.firstOrNull { it.id == id }
            val serviceName = project?.name?.trim().orEmpty()
            _actingName.value = serviceName.ifEmpty { null }
            _actionError.value = null

            if (serviceName.isNotEmpty()) {
                val stopResult = service?.systemctlStop(serviceName)
                if (stopResult?.isFailure == true) {
                    val message = stopResult.exceptionOrNull()?.message
                    if (!isIgnorableSystemctlError(message)) {
                        _actionError.value = "删除前停止服务失败: $message"
                        _actingName.value = null
                        return@launch
                    }
                }

                val disableResult = service?.systemctlDisable(serviceName)
                if (disableResult?.isFailure == true) {
                    val message = disableResult.exceptionOrNull()?.message
                    if (!isIgnorableSystemctlError(message)) {
                        _actionError.value = "删除前关闭自启动失败: $message"
                        _actingName.value = null
                        return@launch
                    }
                }
            }

            service?.deleteProject(id)?.onSuccess {
                _projects.value = _projects.value.filter { it.id != id }
                _total.value = (_total.value - 1).coerceAtLeast(0)
            }?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actingName.value = null
        }
    }

    private fun isIgnorableSystemctlError(message: String?): Boolean {
        val msg = message?.lowercase().orEmpty()
        return msg.contains("could not be found") ||
            msg.contains("not-found") ||
            msg.contains("not found") ||
            msg.contains("not loaded") ||
            msg.contains("no such file")
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
