package org.example.project

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import org.example.project.ui.screens.*

sealed class Screen {
    object Home : Screen()
    data class PanelDetail(val panelId: String) : Screen()
    object AddPanel : Screen()
    object Stats : Screen()
    data class WebsiteList(val panelId: String) : Screen()
    data class WebsiteDetail(val panelId: String, val websiteId: Long) : Screen()
    data class AddWebsite(val panelId: String) : Screen()
    data class DatabaseList(val panelId: String) : Screen()
    data class DatabaseDetail(val panelId: String, val databaseId: Long) : Screen()
    data class AddDatabase(val panelId: String) : Screen()
    data class Terminal(val panelId: String, val sshId: Long = 0) : Screen()
    data class Firewall(val panelId: String) : Screen()
    data class SSLCert(val panelId: String) : Screen()
    data class FileManager(val panelId: String) : Screen()
    data class PanelSettings(val panelId: String) : Screen()
    data class SoftwareStore(val panelId: String) : Screen()
    data class SystemMonitor(val panelId: String) : Screen()
    data class LogView(val panelId: String) : Screen()
    data class CronTask(val panelId: String) : Screen()
    data class BackupManagement(val panelId: String) : Screen()
    data class ConfigEditor(val panelId: String, val fileName: String = "配置编辑器", val filePath: String = "") : Screen()
    data class ActionPage(
        val title: String,
        val description: String,
        val buttonText: String
    ) : Screen()
    data class DeleteWebsiteConfirm(val panelId: String, val websiteId: String) : Screen()
    data class Login(val panelId: String) : Screen()
    data class TaskCenter(val panelId: String) : Screen()
    data class ProjectList(val panelId: String) : Screen()
    data class DatabaseServer(val panelId: String) : Screen()
    data class DatabaseUser(val panelId: String) : Screen()
    data class WebHookManagement(val panelId: String) : Screen()
    data class SshHost(val panelId: String) : Screen()
    data class BackupStorage(val panelId: String) : Screen()
    data class TokenManagement(val panelId: String) : Screen()
    data class UserManagement(val panelId: String) : Screen()
    data class RedisKey(val panelId: String) : Screen()
    data class CertConfig(val panelId: String) : Screen()
    data class WebsiteStat(val panelId: String) : Screen()
    data class FirewallScan(val panelId: String) : Screen()
}

@Composable
fun App() {
    var navigationStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }

    val currentScreen = navigationStack.last()

    val navigateTo: (Screen) -> Unit = { screen ->
        navigationStack = navigationStack + screen
    }

    val navigateBack: () -> Unit = {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    val popToWebsiteList: (String) -> Unit = { panelId ->
        val targetIndex = navigationStack.indexOfLast { it is Screen.WebsiteList }
        navigationStack = if (targetIndex >= 0) {
            navigationStack.take(targetIndex + 1)
        } else {
            listOf(Screen.Home)
        }
    }

    // Handle system back button/gesture
    BackHandler(enabled = navigationStack.size > 1) {
        navigateBack()
    }

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                onPanelClick = { panelId -> navigateTo(Screen.PanelDetail(panelId)) },
                onAddClick = { navigateTo(Screen.AddPanel) }
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
                        "cron" -> navigateTo(Screen.CronTask(screen.panelId))
                        "tasks" -> navigateTo(Screen.TaskCenter(screen.panelId))
                        "backup" -> navigateTo(Screen.BackupManagement(screen.panelId))
                        "config" -> navigateTo(Screen.ConfigEditor(screen.panelId, "panel.conf"))
                        "db_servers" -> navigateTo(Screen.DatabaseServer(screen.panelId))
                        "db_users" -> navigateTo(Screen.DatabaseUser(screen.panelId))
                        "webhooks" -> navigateTo(Screen.WebHookManagement(screen.panelId))
                        "ssh" -> navigateTo(Screen.SshHost(screen.panelId))
                        "backup_storage" -> navigateTo(Screen.BackupStorage(screen.panelId))
                        "ssl" -> navigateTo(Screen.SSLCert(screen.panelId))
                        "tokens" -> navigateTo(Screen.TokenManagement(screen.panelId))
                        "users" -> navigateTo(Screen.UserManagement(screen.panelId))
                        "redis_keys" -> navigateTo(Screen.RedisKey(screen.panelId))
                        "cert_config" -> navigateTo(Screen.CertConfig(screen.panelId))
                        "website_stat" -> navigateTo(Screen.WebsiteStat(screen.panelId))
                        "firewall_scan" -> navigateTo(Screen.FirewallScan(screen.panelId))
                        else -> {}
                    }
                }
            )
        }
        is Screen.AddPanel -> {
            AddPanelScreen(
                onBackClick = navigateBack,
                onSaveClick = navigateBack,
                onSessionSaved = { panelId -> navigateTo(Screen.Login(panelId)) }
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
                onMenuClick = { menu ->
                    when (menu) {
                        "config" -> navigateTo(Screen.ConfigEditor(screen.panelId, "site.conf"))
                        "rewrite" -> navigateTo(Screen.ConfigEditor(screen.panelId, "rewrite.rules"))
                        "ssl" -> navigateTo(Screen.SSLCert(screen.panelId))
                        "directory" -> navigateTo(Screen.FileManager(screen.panelId))
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
                onDatabaseClick = { dbId ->
                    navigateTo(Screen.DatabaseDetail(screen.panelId, dbId.toLongOrNull() ?: 0L))
                }
            )
        }
        is Screen.DatabaseDetail -> {
            DatabaseDetailScreen(
                panelId = screen.panelId,
                databaseId = screen.databaseId,
                onBackClick = navigateBack,
                onEditConfigClick = { navigateTo(Screen.ConfigEditor(screen.panelId, "database.conf")) }
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
                onBackClick = navigateBack,
                onAddClick = {
                    navigateTo(Screen.ActionPage("添加防火墙规则", "创建新的允许/拒绝规则", "保存规则"))
                }
            )
        }
        is Screen.SSLCert -> {
            SSLCertScreen(
                panelId = screen.panelId,
                onBackClick = navigateBack,
                onAddClick = {
                    navigateTo(Screen.ActionPage("申请 SSL 证书", "为网站添加新证书", "保存证书"))
                }
            )
        }
        is Screen.FileManager -> {
            FileManagerScreen(
                panelId = screen.panelId,
                onBackClick = navigateBack,
                onAddClick = {},
                onOpenFile = { filePath ->
                    navigateTo(Screen.ConfigEditor(screen.panelId, filePath.substringAfterLast('/'), filePath))
                }
            )
        }
        is Screen.PanelSettings -> {
            PanelSettingsScreen(
                panelId = screen.panelId,
                onBackClick = navigateBack,
                onNavigateToTokenManagement = { navigateTo(Screen.TokenManagement(screen.panelId)) },
                onNavigateToUserManagement = { navigateTo(Screen.UserManagement(screen.panelId)) }
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
        is Screen.CronTask -> {
            CronTaskScreen(
                panelId = screen.panelId,
                onBack = navigateBack,
                onAddClick = {
                    navigateTo(Screen.ActionPage("添加计划任务", "设置新的自动化任务", "保存任务"))
                }
            )
        }
        is Screen.BackupManagement -> {
            BackupManagementScreen(
                panelId = screen.panelId,
                onBack = navigateBack,
                onAddClick = {
                    navigateTo(Screen.ActionPage("创建备份", "手动创建资源备份", "开始备份"))
                }
            )
        }
        is Screen.ActionPage -> {
            ActionPlaceholderScreen(
                title = screen.title,
                description = screen.description,
                buttonText = screen.buttonText,
                onBackClick = navigateBack,
                onSubmitClick = navigateBack
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
        is Screen.DatabaseServer -> {
            DatabaseServerScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
        is Screen.DatabaseUser -> {
            DatabaseUserScreen(
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
        is Screen.BackupStorage -> {
            BackupStorageScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
        is Screen.TokenManagement -> {
            TokenManagementScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
        is Screen.UserManagement -> {
            UserManagementScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
        is Screen.RedisKey -> {
            RedisKeyScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
        is Screen.CertConfig -> {
            CertConfigScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
        is Screen.WebsiteStat -> {
            WebsiteStatScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
        is Screen.FirewallScan -> {
            FirewallScanScreen(
                panelId = screen.panelId,
                onBack = navigateBack
            )
        }
    }
}
