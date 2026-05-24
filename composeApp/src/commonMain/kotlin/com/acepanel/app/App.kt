package com.acepanel.app

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.acepanel.app.ui.components.GlobalFeedbackHost
import com.acepanel.app.ui.screens.*

sealed class Screen {
    object Home : Screen()
    data class PanelDetail(val panelId: String) : Screen()
    data class AddPanel(val panelId: String? = null) : Screen()
    object Stats : Screen()
    data class WebsiteList(val panelId: String) : Screen()
    data class WebsiteDetail(val panelId: String, val websiteId: Long) : Screen()
    data class AddWebsite(val panelId: String) : Screen()
    data class DatabaseList(val panelId: String) : Screen()
    data class DatabaseDetail(val panelId: String, val serverId: Long, val databaseName: String) : Screen()
    data class AddDatabase(val panelId: String) : Screen()
    data class Terminal(val panelId: String, val sshId: Long = 0) : Screen()
    data class Firewall(val panelId: String) : Screen()
    data class SSLCert(val panelId: String) : Screen()
    data class FileManager(
        val panelId: String,
        val path: String = "/",
        val pathHistory: List<String> = emptyList(),
        val scrollPositions: Map<String, FileListPosition> = emptyMap()
    ) : Screen()
    data class PanelSettings(val panelId: String) : Screen()
    data class SoftwareStore(val panelId: String) : Screen()
    data class SystemMonitor(val panelId: String) : Screen()
    data class LogView(val panelId: String) : Screen()
    data class BackupManagement(val panelId: String) : Screen()
    data class ConfigEditor(val panelId: String, val fileName: String = "配置编辑器", val filePath: String = "") : Screen()
    data class DeleteWebsiteConfirm(val panelId: String, val websiteId: String) : Screen()
    data class Login(
        val panelId: String,
        val username: String = "",
        val password: String = "",
        val autoLogin: Boolean = false
    ) : Screen()
    data class TaskCenter(val panelId: String) : Screen()
    data class ProjectList(val panelId: String) : Screen()
    data class WebHookManagement(val panelId: String) : Screen()
    data class SshHost(val panelId: String) : Screen()
}

data class FileListPosition(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
    val anchorPath: String? = null
)

private fun screenStateKey(screen: Screen): String {
    return when (screen) {
        Screen.Home -> "home"
        is Screen.AddPanel -> screen.panelId?.let { "edit-panel:$it" } ?: "add-panel"
        Screen.Stats -> "stats"
        is Screen.PanelDetail -> "panel-detail:${screen.panelId}"
        is Screen.WebsiteList -> "website-list:${screen.panelId}"
        is Screen.WebsiteDetail -> "website-detail:${screen.panelId}:${screen.websiteId}"
        is Screen.AddWebsite -> "add-website:${screen.panelId}"
        is Screen.DatabaseList -> "database-list:${screen.panelId}"
        is Screen.DatabaseDetail -> "database-detail:${screen.panelId}:${screen.serverId}:${screen.databaseName}"
        is Screen.AddDatabase -> "add-database:${screen.panelId}"
        is Screen.Terminal -> "terminal:${screen.panelId}:${screen.sshId}"
        is Screen.Firewall -> "firewall:${screen.panelId}"
        is Screen.SSLCert -> "ssl-cert:${screen.panelId}"
        is Screen.FileManager -> "file-manager:${screen.panelId}:${screen.path}"
        is Screen.PanelSettings -> "panel-settings:${screen.panelId}"
        is Screen.SoftwareStore -> "software-store:${screen.panelId}"
        is Screen.SystemMonitor -> "system-monitor:${screen.panelId}"
        is Screen.LogView -> "log-view:${screen.panelId}"
        is Screen.BackupManagement -> "backup-management:${screen.panelId}"
        is Screen.ConfigEditor -> "config-editor:${screen.panelId}:${screen.filePath}:${screen.fileName}"
        is Screen.DeleteWebsiteConfirm -> "delete-website:${screen.panelId}:${screen.websiteId}"
        is Screen.Login -> "login:${screen.panelId}"
        is Screen.TaskCenter -> "task-center:${screen.panelId}"
        is Screen.ProjectList -> "project-list:${screen.panelId}"
        is Screen.WebHookManagement -> "webhook-management:${screen.panelId}"
        is Screen.SshHost -> "ssh-host:${screen.panelId}"
    }
}

@Composable
fun App() {
    var navigationStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    val saveableStateHolder = rememberSaveableStateHolder()

    val currentScreen = navigationStack.last()

    val navigateTo: (Screen) -> Unit = { screen ->
        navigationStack = navigationStack + screen
    }

    val navigateBack: () -> Unit = {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    val replaceCurrent: (Screen) -> Unit = { screen ->
        navigationStack = navigationStack.dropLast(1) + screen
    }

    fun parentPath(path: String): String? {
        val normalized = path.trimEnd('/').ifEmpty { "/" }
        if (normalized == "/") return null

        return normalized.substringBeforeLast('/', missingDelimiterValue = "")
            .ifEmpty { "/" }
    }

    fun navigateFileBack(screen: Screen.FileManager) {
        val previousPath = screen.pathHistory.lastOrNull() ?: parentPath(screen.path)
        if (previousPath == null || previousPath == screen.path) {
            navigateBack()
            return
        }

        val previousHistory = if (screen.pathHistory.isNotEmpty()) {
            screen.pathHistory.dropLast(1)
        } else {
            emptyList()
        }
        replaceCurrent(screen.copy(path = previousPath, pathHistory = previousHistory))
    }

    fun withFileScrollPosition(
        screen: Screen.FileManager,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
        anchorPath: String? = null
    ): Screen.FileManager {
        return screen.copy(
            scrollPositions = screen.scrollPositions + (
                screen.path to FileListPosition(
                    firstVisibleItemIndex = firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                    anchorPath = anchorPath
                )
            )
        )
    }

    val handleSystemBack: () -> Unit = {
        when (val screen = currentScreen) {
            is Screen.FileManager -> navigateFileBack(screen)
            else -> navigateBack()
        }
    }

    val popToWebsiteList: (String) -> Unit = { panelId ->
        val targetIndex = navigationStack.indexOfLast {
            it is Screen.WebsiteList && it.panelId == panelId
        }
        navigationStack = if (targetIndex >= 0) {
            navigationStack.take(targetIndex + 1)
        } else {
            val panelIndex = navigationStack.indexOfLast {
                it is Screen.PanelDetail && it.panelId == panelId
            }
            if (panelIndex >= 0) {
                navigationStack.take(panelIndex + 1)
            } else {
                navigationStack.dropLast(1).ifEmpty { listOf(Screen.Home) }
            }
        }
    }

    val canNavigateFileBack = currentScreen is Screen.FileManager &&
        (currentScreen.pathHistory.isNotEmpty() || parentPath(currentScreen.path) != null)

    PlatformBackHandler(enabled = navigationStack.size > 1 || canNavigateFileBack) {
        handleSystemBack()
    }

    GlobalFeedbackHost {
        saveableStateHolder.SaveableStateProvider(screenStateKey(currentScreen)) {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    onPanelClick = { panelId -> navigateTo(Screen.PanelDetail(panelId)) },
                    onAddClick = { navigateTo(Screen.AddPanel()) },
                    onPanelEdit = { panelId -> navigateTo(Screen.AddPanel(panelId)) }
                )
            }
            is Screen.PanelDetail -> {
                PanelDetailScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack,
                    onMenuClick = { menu ->
                        when (menu) {
                            "stats" -> navigateTo(Screen.Stats)
                            "websites" -> navigateTo(Screen.WebsiteList(screen.panelId))
                            "databases" -> navigateTo(Screen.DatabaseList(screen.panelId))
                            "projects" -> navigateTo(Screen.ProjectList(screen.panelId))
                            "terminal" -> navigateTo(Screen.Terminal(screen.panelId))
                            "firewall" -> navigateTo(Screen.Firewall(screen.panelId))
                            "files" -> navigateTo(Screen.FileManager(screen.panelId))
                            "settings" -> navigateTo(Screen.PanelSettings(screen.panelId))
                            "software" -> navigateTo(Screen.SoftwareStore(screen.panelId))
                            "monitor" -> navigateTo(Screen.SystemMonitor(screen.panelId))
                            "logs" -> navigateTo(Screen.LogView(screen.panelId))
                            "tasks" -> navigateTo(Screen.TaskCenter(screen.panelId))
                            "backup" -> navigateTo(Screen.BackupManagement(screen.panelId))
                            "webhooks" -> navigateTo(Screen.WebHookManagement(screen.panelId))
                            "ssh" -> navigateTo(Screen.SshHost(screen.panelId))
                            "ssl" -> navigateTo(Screen.SSLCert(screen.panelId))
                            else -> {}
                        }
                    }
                )
            }
            is Screen.AddPanel -> {
                AddPanelScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack,
                    onSaveClick = navigateBack,
                    onSessionSaved = { panelId, username, password, autoLogin ->
                        navigateTo(
                            Screen.Login(
                                panelId = panelId,
                                username = username,
                                password = password,
                                autoLogin = autoLogin
                            )
                        )
                    }
                )
            }
            is Screen.Stats -> {
                StatsScreen(onBack = navigateBack)
            }
            is Screen.WebsiteList -> {
                WebsiteListScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack,
                    onAddClick = { navigateTo(Screen.AddWebsite(screen.panelId)) },
                    onWebsiteClick = { websiteId ->
                        navigateTo(Screen.WebsiteDetail(screen.panelId, websiteId.toLongOrNull() ?: 0L))
                    }
                )
            }
            is Screen.WebsiteDetail -> {
                WebsiteDetailScreen(
                    panelId = screen.panelId,
                    websiteId = screen.websiteId,
                    onBackClick = navigateBack,
                    onMenuClick = { menu, website, webserver ->
                        val siteName = website?.name.orEmpty()
                        val configFile = if (siteName.isNotEmpty()) {
                            val configName = if (webserver.equals("apache", ignoreCase = true)) "apache.conf" else "nginx.conf"
                            "/opt/ace/sites/$siteName/config/$configName"
                        } else {
                            ""
                        }
                        val rewriteFile = if (siteName.isNotEmpty()) {
                            "/opt/ace/sites/$siteName/config/site/010-rewrite.conf"
                        } else {
                            ""
                        }
                        val sitePath = website?.path?.takeIf { it.isNotBlank() } ?: "/"
                        when (menu) {
                            "config" -> navigateTo(Screen.ConfigEditor(screen.panelId, configFile.substringAfterLast('/').ifEmpty { "站点配置" }, configFile))
                            "rewrite" -> navigateTo(Screen.ConfigEditor(screen.panelId, "010-rewrite.conf", rewriteFile))
                            "ssl" -> navigateTo(Screen.SSLCert(screen.panelId))
                            "directory" -> navigateTo(Screen.FileManager(screen.panelId, sitePath))
                            "delete" -> navigateTo(Screen.DeleteWebsiteConfirm(screen.panelId, screen.websiteId.toString()))
                            else -> {}
                        }
                    }
                )
            }
            is Screen.AddWebsite -> {
                AddWebsiteScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack,
                    onSubmitClick = navigateBack
                )
            }
            is Screen.DatabaseList -> {
                DatabaseListScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack,
                    onAddClick = { navigateTo(Screen.AddDatabase(screen.panelId)) },
                    onDatabaseClick = { db ->
                        navigateTo(Screen.DatabaseDetail(screen.panelId, db.server_id, db.name))
                    }
                )
            }
            is Screen.DatabaseDetail -> {
                DatabaseDetailScreen(
                    panelId = screen.panelId,
                    serverId = screen.serverId,
                    databaseName = screen.databaseName,
                    onBackClick = navigateBack,
                    onEditConfigClick = { type ->
                        val filePath = when (type.lowercase()) {
                            "mysql", "mariadb" -> "/opt/ace/server/mysql/conf/my.cnf"
                            "postgresql" -> "/opt/ace/server/postgresql/data/postgresql.conf"
                            "redis" -> "/opt/ace/server/redis/redis.conf"
                            else -> ""
                        }
                        navigateTo(Screen.ConfigEditor(screen.panelId, filePath.substringAfterLast('/').ifEmpty { "数据库配置" }, filePath))
                    }
                )
            }
            is Screen.AddDatabase -> {
                AddDatabaseScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack,
                    onSubmitClick = navigateBack
                )
            }
            is Screen.Terminal -> {
                TerminalScreen(
                    panelId = screen.panelId,
                    sshId = screen.sshId,
                    onBackClick = navigateBack
                )
            }
            is Screen.Firewall -> {
                FirewallScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack
                )
            }
            is Screen.SSLCert -> {
                SSLCertScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack
                )
            }
            is Screen.FileManager -> {
                val scrollPosition = screen.scrollPositions[screen.path] ?: FileListPosition()
                FileManagerScreen(
                    panelId = screen.panelId,
                    initialPath = screen.path,
                    initialFirstVisibleItemIndex = scrollPosition.firstVisibleItemIndex,
                    initialFirstVisibleItemScrollOffset = scrollPosition.firstVisibleItemScrollOffset,
                    restoreAnchorPath = scrollPosition.anchorPath,
                    onBackClick = { firstVisibleItemIndex, firstVisibleItemScrollOffset ->
                        navigateFileBack(withFileScrollPosition(screen, firstVisibleItemIndex, firstVisibleItemScrollOffset))
                    },
                    onOpenDirectory = { path, firstVisibleItemIndex, firstVisibleItemScrollOffset ->
                        val currentWithPosition = withFileScrollPosition(
                            screen,
                            firstVisibleItemIndex,
                            firstVisibleItemScrollOffset,
                            anchorPath = path
                        )
                        replaceCurrent(
                            currentWithPosition.copy(
                                path = path,
                                pathHistory = currentWithPosition.pathHistory + currentWithPosition.path
                            )
                        )
                    },
                    onOpenFile = { filePath, firstVisibleItemIndex, firstVisibleItemScrollOffset ->
                        replaceCurrent(
                            withFileScrollPosition(
                                screen,
                                firstVisibleItemIndex,
                                firstVisibleItemScrollOffset,
                                anchorPath = filePath
                            )
                        )
                        navigateTo(Screen.ConfigEditor(screen.panelId, filePath.substringAfterLast('/'), filePath))
                    }
                )
            }
            is Screen.PanelSettings -> {
                PanelSettingsScreen(
                    panelId = screen.panelId,
                    onBackClick = navigateBack
                )
            }
            is Screen.SoftwareStore -> {
                SoftwareStoreScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack
                )
            }
            is Screen.SystemMonitor -> {
                SystemMonitorScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack
                )
            }
            is Screen.LogView -> {
                LogViewScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack
                )
            }
            is Screen.BackupManagement -> {
                BackupManagementScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack
                )
            }
            is Screen.DeleteWebsiteConfirm -> {
                DeleteWebsiteConfirmScreen(
                    panelId = screen.panelId,
                    websiteId = screen.websiteId,
                    onBackClick = navigateBack,
                    onConfirmDelete = { popToWebsiteList(screen.panelId) }
                )
            }
            is Screen.ConfigEditor -> {
                ConfigEditorScreen(
                    panelId = screen.panelId,
                    filePath = screen.filePath,
                    fileName = screen.fileName,
                    onBack = navigateBack,
                    onSaveClick = navigateBack
                )
            }
            is Screen.Login -> {
                LoginScreen(
                    panelId = screen.panelId,
                    initialUsername = screen.username,
                    initialPassword = screen.password,
                    autoLogin = screen.autoLogin,
                    onBackClick = navigateBack,
                    onLoginSuccess = { navigateTo(Screen.PanelDetail(screen.panelId)) }
                )
            }
            is Screen.TaskCenter -> {
                TaskCenterScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack
                )
            }
            is Screen.ProjectList -> {
                ProjectListScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack
                )
            }
            is Screen.WebHookManagement -> {
                WebHookManagementScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack
                )
            }
            is Screen.SshHost -> {
                SshHostScreen(
                    panelId = screen.panelId,
                    onBack = navigateBack,
                    onConnect = { sshId -> navigateTo(Screen.Terminal(screen.panelId, sshId)) }
                )
            }
        }
        }
    }
}
