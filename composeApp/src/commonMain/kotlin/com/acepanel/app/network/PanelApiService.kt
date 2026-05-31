package com.acepanel.app.network

import io.ktor.client.plugins.websocket.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.acepanel.app.data.*

/**
 * 面板 API 服务，封装所有接口调用
 * 每个 PanelConfig 对应一个实例
 */
class PanelApiService(config: PanelConfig) {

    private val client = ApiClient(config)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    // =================== 通用工具 ===================

    /**
     * 解析 API 响应，提取 data 字段
     * 后端成功响应格式：{"msg":"success","data":<T>}
     * 后端错误响应格式：{"msg":"error message"}（HTTP 4xx/5xx）
     * 先读 bodyAsText 避免二次消费 body 导致异常
     */
    private suspend inline fun <reified T> HttpResponse.parseData(): Result<T> {
        return try {
            val text = bodyAsText()
            if (status.isSuccess()) {
                val response = json.decodeFromString<ApiResponse<T>>(text)
                if (response.data != null) {
                    Result.success(response.data)
                } else {
                    val msg = extractMsgFromText(text)
                    Result.failure(Exception(msg.ifEmpty { "服务器未返回数据" }))
                }
            } else {
                val msg = extractMsgFromText(text)
                val fallback = when (status.value) {
                    418 -> "访问被拒绝（HTTP 418）—— 可能原因：① 安全入口路径未验证 ② 面板设置了 IP/域名绑定，当前设备不在允许列表中"
                    401 -> "未授权（HTTP 401）：${status.description.ifEmpty { "请检查 Token 是否有效" }}"
                    307 -> "HTTP 307 临时重定向：请检查面板协议是否应为 https，以及入口路径是否填写正确（不要前后多余 /）"
                    else -> "HTTP ${status.value}${if (status.description.isNotEmpty()) ": ${status.description}" else ""}"
                }
                Result.failure(Exception(msg.ifEmpty { fallback }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 从 JSON 文本中提取 msg 字段（后端错误消息字段名） */
    private fun extractMsgFromText(text: String): String {
        return try {
            json.decodeFromString<com.acepanel.app.data.ApiErrorBody>(text).msg
        } catch (_: Exception) { "" }
    }

    private fun formatErrorBody(text: String): String {
        return extractMsgFromText(text).ifBlank { text }
    }

    private fun isEntranceDenied(message: String?): Boolean {
        val text = message.orEmpty()
        return text.contains("HTTP 418") || text.contains("访问被拒绝")
    }

    private fun loginEntranceHint(action: String, cause: String? = null): String {
        val entrance = client.config.entrance.trim().trim('/')
        val entranceText = if (entrance.isEmpty()) "未填写" else "/$entrance"
        val hostText = "${client.config.host}:${client.config.port}"
        val userAgentText = client.config.userAgent.trim().ifBlank {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
        }
        val causeText = cause?.takeIf { it.isNotBlank() }?.let { "\n原始错误：$it" }.orEmpty()
        return "$action 被面板安全入口拦截。\n" +
            "请检查客户端面板配置：安全入口当前为「$entranceText」，主机为「$hostText」。\n" +
            "如果这个地址在浏览器能打开但客户端仍然 418，请重点检查面板 UA/IP/域名绑定；当前客户端 UA 为「$userAgentText」。" +
            causeText
    }

    private suspend fun prepareSessionEntrance(action: String): Result<Unit> {
        if (client.config.authMode != "session") return Result.success(Unit)
        return client.initEntrance()
            .fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(Exception(loginEntranceHint(action, it.message))) }
            )
    }

    private suspend fun <T> withSessionEntrance(action: String, block: suspend () -> Result<T>): Result<T> {
        prepareSessionEntrance(action).onFailure { return Result.failure(it) }
        val first = block()
        if (first.isSuccess || client.config.authMode != "session" || !isEntranceDenied(first.exceptionOrNull()?.message)) {
            return first
        }

        prepareSessionEntrance(action).onFailure { return Result.failure(it) }
        val second = block()
        return if (second.isFailure && isEntranceDenied(second.exceptionOrNull()?.message)) {
            Result.failure(Exception(loginEntranceHint(action, second.exceptionOrNull()?.message)))
        } else {
            second
        }
    }

    // =================== 登录/鉴权 ===================

    /** 检查是否已登录（Session 模式） */
    suspend fun isLogin(sessionCookie: String? = null): Result<Boolean> {
        return try {
            client.get("/api/user/is_login", sessionCookie = sessionCookie).parseData<Boolean>()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取面板基本信息（无需鉴权，用于连通性测试） */
    suspend fun getPanelInfo(): Result<PanelInfo> {
        return try {
            client.get("/api/home/panel").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取登录 RSA 公钥 */
    suspend fun getPublicKey(sessionCookie: String? = null): Result<String> {
        return withSessionEntrance("获取登录公钥") {
            try {
            val resp = client.get("/api/user/key", sessionCookie = sessionCookie)
            // 后端 data 直接返回 PEM 字符串，不是对象
            resp.parseData<String>()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** 获取验证码状态 */
    suspend fun getCaptcha(sessionCookie: String? = null): Result<CaptchaResponse> {
        return withSessionEntrance("获取登录验证码") {
            try {
                client.get("/api/user/captcha", sessionCookie = sessionCookie).parseData()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** 检查用户是否开启 2FA（明文用户名 query） */
    suspend fun isTwoFaEnabled(username: String): Result<Boolean> {
        return withSessionEntrance("检查两步验证") {
            try {
                client.get("/api/user/is_2fa", mapOf("username" to username)).parseData()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Session 登录
     * @param encryptedUsername RSA-OAEP-SHA512 加密并 Base64 的用户名
     * @param encryptedPassword RSA-OAEP-SHA512 加密并 Base64 的密码
     * @return Pair<成功, 响应头中的 Cookie>
     */
    suspend fun login(
        encryptedUsername: String,
        encryptedPassword: String,
        passCode: String = "",
        captchaCode: String = "",
        sessionCookie: String? = null
    ): Result<String> {
        return withSessionEntrance("账号密码登录") {
            try {
                val req = LoginRequest(
                    username = encryptedUsername,
                    password = encryptedPassword,
                    pass_code = passCode,
                    captcha_code = captchaCode
                )
                val resp = client.post(
                    "/api/user/login",
                    json.encodeToString(req),
                    sessionCookie = sessionCookie
                )
                if (resp.status.isSuccess()) {
                    // client.post() 内 mergeSetCookies 已将多个 Set-Cookie 合并、去掉 Path/HttpOnly 等属性
                    // 直接使用已清理的 storedSessionCookie 持久化，供其他 ViewModel 跨实例共享
                    val cleanCookie = client.storedSessionCookie ?: ""
                    if (cleanCookie.isNotEmpty()) {
                        SessionCookieStore.set(client.config.id, cleanCookie)
                        PanelRepository.saveSessionCookie(client.config.id, cleanCookie)
                    }
                    Result.success(cleanCookie)
                } else {
                    val body = resp.bodyAsText()
                    val msg = extractMsgFromText(body).ifEmpty { "HTTP ${resp.status.value}" }
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** 退出登录 */
    suspend fun logout(sessionCookie: String? = null): Result<Unit> {
        return try {
            client.post("/api/user/logout", sessionCookie = sessionCookie)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取当前用户信息 */
    suspend fun getUserInfo(): Result<UserInfo> {
        return try {
            client.get("/api/user/info").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 系统信息 ===================

    /** 获取系统基本信息（hostname、OS、uptime 等） */
    suspend fun getSystemInfo(): Result<SystemInfo> {
        return try {
            client.get("/api/home/system_info").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取实时资源使用率（CPU/内存/磁盘/网络） */
    suspend fun getCurrentUsage(
        nets: List<String> = emptyList(),
        disks: List<String> = emptyList()
    ): Result<CurrentUsage> {
        return try {
            val body = json.encodeToString(CurrentUsageRequest(nets = nets, disks = disks))
            client.post("/api/home/current", body).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取资源计数（网站/数据库/计划任务数量） */
    suspend fun getCountInfo(): Result<CountInfo> {
        return try {
            client.get("/api/home/count_info").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 网站管理 ===================

    /** 获取网站列表 */
    suspend fun getWebsites(
        type: String = "all",
        page: Int = 1,
        limit: Int = 20
    ): Result<WebsiteListResponse> {
        return try {
            client.get(
                "/api/website",
                mapOf("type" to type, "page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 切换网站启停状态 */
    suspend fun setWebsiteStatus(id: Long, status: Boolean): Result<Unit> {
        return try {
            val body = json.encodeToString(WebsiteStatusRequest(id = id, status = status))
            val resp = client.post("/api/website/$id/status", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除网站 */
    suspend fun deleteWebsite(id: Long, deletePath: Boolean = false, deleteDb: Boolean = false): Result<Unit> {
        return try {
            val body = json.encodeToString(DeleteWebsiteRequest(id = id, path = deletePath, db = deleteDb))
            val resp = client.delete("/api/website/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 数据库管理 ===================

    /** 获取数据库列表 */
    suspend fun getDatabases(page: Int = 1, limit: Int = 20): Result<DatabaseListResponse> {
        return try {
            client.get(
                "/api/database",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取数据库服务器列表 */
    suspend fun getDatabaseServers(page: Int = 1, limit: Int = 20): Result<DatabaseServerListResponse> {
        return try {
            client.get(
                "/api/database_server",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除数据库 */
    suspend fun deleteDatabase(serverId: Long, name: String): Result<Unit> {
        return try {
            val body = """{"server_id":$serverId,"name":"$name"}"""
            val resp = client.delete("/api/database", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== Token 管理 ===================

    /** 获取 Token 列表 */
    suspend fun getTokens(userId: Long, page: Int = 1, limit: Int = 20): Result<UserTokenListResponse> {
        return try {
            client.get(
                "/api/user_tokens",
                mapOf("user_id" to userId.toString(), "page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 创建 Token */
    suspend fun createToken(userId: Long, ips: List<String>, expiredAt: Long): Result<UserTokenItem> {
        return try {
            val body = json.encodeToString(CreateTokenRequest(user_id = userId, ips = ips, expired_at = expiredAt))
            client.post("/api/user_tokens", body).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除 Token */
    suspend fun deleteToken(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/user_tokens/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 防火墙 ===================

    /** 获取防火墙状态 */
    suspend fun getFirewallStatus(): Result<Boolean> {
        return try {
            client.get("/api/firewall/status").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 切换防火墙开关 */
    suspend fun setFirewallStatus(status: Boolean): Result<Unit> {
        return try {
            val body = """{"status":$status}"""
            val resp = client.post("/api/firewall/status", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取防火墙端口规则列表 */
    suspend fun getFirewallRules(page: Int = 1, limit: Int = 50): Result<FirewallRuleListResponse> {
        return try {
            client.get(
                "/api/firewall/rule",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除防火墙规则 */
    suspend fun deleteFirewallRule(rule: CreateFirewallRuleRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(rule)
            val resp = client.delete("/api/firewall/rule", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 计划任务 ===================

    /** 获取计划任务列表 */
    suspend fun getCronTasks(page: Int = 1, limit: Int = 20): Result<CronTaskListResponse> {
        return try {
            client.get(
                "/api/cron",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 切换计划任务状态 */
    suspend fun setCronTaskStatus(id: Long, status: Boolean): Result<Unit> {
        return try {
            val body = json.encodeToString(CronStatusRequest(id = id, status = status))
            val resp = client.post("/api/cron/$id/status", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除计划任务 */
    suspend fun deleteCronTask(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/cron/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取计划任务详情 */
    suspend fun getCronTask(id: Long): Result<CronTaskApiItem> {
        return try {
            client.get("/api/cron/$id").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 日志 ===================

    /** 获取日志列表 */
    suspend fun getLogs(type: String, limit: Int = 100, date: String = ""): Result<List<LogApiEntry>> {
        return try {
            val params = mutableMapOf("type" to type, "limit" to limit.toString())
            if (date.isNotEmpty()) params["date"] = date
            client.get("/api/log/list", params).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取日志日期列表 */
    suspend fun getLogDates(type: String): Result<List<String>> {
        return try {
            client.get("/api/log/dates", mapOf("type" to type)).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取 SSH 登录日志 */
    suspend fun getSshLogs(limit: Int = 100): Result<List<SshLogApiItem>> {
        return try {
            client.get("/api/log/ssh", mapOf("limit" to limit.toString())).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 监控 ===================

    /** 获取 Top 进程（type: cpu/memory/disk_io） */
    suspend fun getTopProcesses(type: String = "cpu"): Result<List<ProcessStatItem>> {
        return try {
            client.get("/api/home/top_processes", mapOf("type" to type)).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 面板设置 ===================

    /** 获取面板设置 */
    suspend fun getSettings(): Result<PanelSettingData> {
        return try {
            client.get("/api/setting").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 更新面板设置 */
    suspend fun updateSettings(setting: PanelSettingData): Result<UpdateSettingResponse> {
        return try {
            client.post("/api/setting", json.encodeToString(setting)).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取便签内容（返回字符串本体） */
    suspend fun getMemo(): Result<String> {
        return try {
            client.get("/api/setting/memo").parseData<String>()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 保存便签内容 */
    suspend fun setMemo(content: String): Result<Unit> {
        return try {
            val body = """{"content":${json.encodeToString(content)}}"""
            val resp = client.post("/api/setting/memo", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 软件商店 ===================

    /** 获取应用分类 */
    suspend fun getAppCategories(): Result<List<AppCategoryItem>> {
        return try {
            client.get("/api/app/categories").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取应用列表 */
    suspend fun getApps(
        page: Int = 1,
        limit: Int = 20,
        category: String = "",
        query: String = "",
        installed: Boolean = false
    ): Result<AppListResponse> {
        return try {
            val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
            if (category.isNotEmpty()) params["category"] = category
            if (query.isNotEmpty()) params["query"] = query
            if (installed) params["installed"] = "true"
            client.get("/api/app/list", params).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 安装应用 */
    suspend fun installApp(slug: String, channel: String = "stable"): Result<Unit> {
        return try {
            val body = json.encodeToString(AppInstallRequest(slug = slug, channel = channel))
            val resp = client.post("/api/app/install", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 卸载应用 */
    suspend fun uninstallApp(slug: String): Result<Unit> {
        return try {
            val body = json.encodeToString(AppSlugRequest(slug = slug))
            val resp = client.post("/api/app/uninstall", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApp(slug: String): Result<Unit> {
        return try {
            val body = json.encodeToString(AppSlugRequest(slug = slug))
            val resp = client.post("/api/app/update", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 网站 / 数据库创建 ===================

    /** 获取已安装运行环境（PHP/DB/Web服务器） */
    suspend fun getInstalledEnvironment(): Result<InstalledEnvironment> {
        return try {
            client.get("/api/home/installed_environment").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 创建网站 */
    suspend fun createWebsite(req: CreateWebsiteRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/website", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else {
                val errBody = resp.bodyAsText()
                Result.failure(Exception("创建失败: $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 创建数据库 */
    suspend fun createDatabaseEntry(req: CreateDatabaseRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/database", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else {
                val errBody = resp.bodyAsText()
                Result.failure(Exception("创建失败: $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 网站详情 ===================

    /** 获取网站完整配置 */
    suspend fun getWebsiteDetail(id: Long): Result<WebsiteDetailData> {
        return try {
            client.get("/api/website/$id").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 任务中心 ===================

    /** 检查是否有运行中任务 */
    suspend fun getTaskStatus(): Result<Boolean> {
        return try {
            client.get("/api/task/status").parseData<TaskStatusResponse>().map { it.task }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取任务列表 */
    suspend fun getTasks(page: Int = 1, limit: Int = 20): Result<TaskListResponse> {
        return try {
            client.get(
                "/api/task",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除任务 */
    suspend fun deleteTask(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/task/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 项目管理 ===================

    /** 获取项目列表 */
    suspend fun getProjects(type: String = "all", page: Int = 1, limit: Int = 20): Result<ProjectListResponse> {
        return try {
            val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
            if (type.isNotEmpty() && type != "all") params["type"] = type
            client.get("/api/project", params).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除项目 */
    suspend fun deleteProject(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/project/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取项目详情 */
    suspend fun getProject(id: Long): Result<ProjectListItem> {
        return try {
            client.get("/api/project/$id").parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 更新项目 */
    suspend fun updateProject(id: Long, req: UpdateProjectRequest): Result<Unit> {
        return try {
            val resp = client.put("/api/project/$id", json.encodeToString(req.copy(id = id)))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(formatErrorBody(resp.bodyAsText())))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== Systemctl ===================

    /** 启动服务 */
    suspend fun systemctlStart(service: String): Result<Unit> {
        return try {
            val body = """{"service":"$service"}"""
            val resp = client.post("/api/systemctl/start", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 停止服务 */
    suspend fun systemctlStop(service: String): Result<Unit> {
        return try {
            val body = """{"service":"$service"}"""
            val resp = client.post("/api/systemctl/stop", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 重启服务 */
    suspend fun systemctlRestart(service: String): Result<Unit> {
        return try {
            val body = """{"service":"$service"}"""
            val resp = client.post("/api/systemctl/restart", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 查询服务运行状态（active=true） */
    suspend fun systemctlStatus(service: String): Result<Boolean> {
        return try {
            client.get("/api/systemctl/status", mapOf("service" to service))
                .parseData<Boolean>()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 查询服务是否已开机自启 */
    suspend fun systemctlIsEnabled(service: String): Result<Boolean> {
        return try {
            client.get("/api/systemctl/is_enabled", mapOf("service" to service))
                .parseData<Boolean>()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 开启服务自启动 */
    suspend fun systemctlEnable(service: String): Result<Unit> {
        return try {
            val body = """{"service":"$service"}"""
            val resp = client.post("/api/systemctl/enable", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 关闭服务自启动 */
    suspend fun systemctlDisable(service: String): Result<Unit> {
        return try {
            val body = """{"service":"$service"}"""
            val resp = client.post("/api/systemctl/disable", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== SSL 证书 ===================

    /** 获取证书列表 */
    suspend fun getCerts(page: Int = 1, limit: Int = 20): Result<CertListResponse> {
        return try {
            client.get(
                "/api/cert/cert",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 续期证书 */
    suspend fun renewCert(id: Long): Result<Unit> {
        return try {
            val resp = client.post("/api/cert/cert/$id/renew", "{}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除证书 */
    suspend fun deleteCert(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/cert/cert/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 文件管理 ===================

    /** 获取文件/目录列表 */
    suspend fun getFileList(path: String, page: Int = 1, limit: Int = 100): Result<FileListResponse> {
        return try {
            client.get(
                "/api/file/list",
                mapOf("path" to path, "page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取文件内容（返回 Base64 编码） */
    suspend fun getFileContent(path: String): Result<FileContentResponse> {
        return try {
            client.get("/api/file/content", mapOf("path" to path)).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 读取文件尾部或 systemd 服务日志 */
    suspend fun tailFile(path: String = "", service: String = "", offset: Int = 0, limit: Int = 500): Result<FileTailResponse> {
        return try {
            val params = mutableMapOf(
                "offset" to offset.toString(),
                "limit" to limit.toString()
            )
            if (path.isNotBlank()) params["path"] = path
            if (service.isNotBlank()) params["service"] = service
            client.get("/api/file/tail", params).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 保存文件内容（content 为原始文本） */
    suspend fun saveFile(path: String, content: String): Result<Unit> {
        return try {
            val body = json.encodeToString(FileSaveRequest(path, content))
            val resp = client.post("/api/file/save", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 新建文件或目录 */
    suspend fun createFile(path: String, dir: Boolean): Result<Unit> {
        return try {
            val body = """{"path":${json.encodeToString(path)},"dir":$dir}"""
            val resp = client.post("/api/file/create", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除文件或目录 */
    suspend fun deleteFile(path: String): Result<Unit> {
        return try {
            val body = """{"path":${json.encodeToString(path)}}"""
            val resp = client.post("/api/file/delete", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 移动/重命名文件或目录 */
    suspend fun moveFiles(items: List<FileControlRequest>): Result<Unit> {
        return try {
            val resp = client.post("/api/file/move", json.encodeToString(items))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 复制文件或目录 */
    suspend fun copyFiles(items: List<FileControlRequest>): Result<Unit> {
        return try {
            val resp = client.post("/api/file/copy", json.encodeToString(items))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 修改权限和属主 */
    suspend fun updateFilePermission(path: String, mode: String, owner: String, group: String): Result<Unit> {
        return try {
            val resp = client.post(
                "/api/file/permission",
                json.encodeToString(FilePermissionRequest(path, mode, owner, group))
            )
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 压缩文件或目录 */
    suspend fun compressFiles(dir: String, paths: List<String>, file: String): Result<Unit> {
        return try {
            val resp = client.post(
                "/api/file/compress",
                json.encodeToString(FileCompressRequest(dir, paths, file))
            )
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 解压文件 */
    suspend fun unCompressFile(file: String, path: String): Result<Unit> {
        return try {
            val resp = client.post(
                "/api/file/un_compress",
                json.encodeToString(FileUnCompressRequest(file, path))
            )
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 远程下载文件到当前目录 */
    suspend fun remoteDownloadFile(path: String, url: String): Result<Unit> {
        return try {
            val resp = client.post(
                "/api/file/remote_download",
                json.encodeToString(FileRemoteDownloadRequest(path, url))
            )
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 上传本地文件到远程完整路径 */
    suspend fun uploadFile(path: String, fileName: String, bytes: ByteArray, force: Boolean = false): Result<Unit> {
        return try {
            val (body, boundary) = buildUploadMultipartBody(path, fileName, bytes, force)
            val resp = client.postBytes(
                "/api/file/upload",
                body,
                ContentType.MultiPart.FormData.withParameter("boundary", boundary)
            )
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 下载远程文件，返回文件字节 */
    suspend fun downloadFile(path: String): Result<ByteArray> {
        return try {
            val resp = client.get("/api/file/download", mapOf("path" to path))
            if (resp.status.isSuccess()) Result.success(resp.bodyAsBytes())
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildUploadMultipartBody(
        path: String,
        fileName: String,
        bytes: ByteArray,
        force: Boolean
    ): Pair<ByteArray, String> {
        val boundary = "AcePanelBoundary${currentTimeSeconds()}${bytes.size}"
        val parts = mutableListOf<ByteArray>()

        fun appendText(text: String) {
            parts += text.encodeToByteArray()
        }

        fun appendField(name: String, value: String) {
            appendText("--$boundary\r\n")
            appendText("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
            appendText("$value\r\n")
        }

        appendField("path", path)
        appendField("force", force.toString())

        val safeFileName = fileName.replace("\"", "%22")
        appendText("--$boundary\r\n")
        appendText("Content-Disposition: form-data; name=\"file\"; filename=\"$safeFileName\"\r\n")
        appendText("Content-Type: application/octet-stream\r\n\r\n")
        parts += bytes
        appendText("\r\n--$boundary--\r\n")

        val totalSize = parts.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        parts.forEach { part ->
            part.copyInto(result, offset)
            offset += part.size
        }
        return result to boundary
    }

    // =================== 备份管理 ===================

    /** 获取备份列表 */
    suspend fun getBackups(type: String, page: Int = 1, limit: Int = 50): Result<BackupListResponse> {
        return try {
            client.get(
                "/api/backup/$type",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 删除备份文件 */
    suspend fun deleteBackup(type: String, file: String): Result<Unit> {
        return try {
            val body = """{"file":${json.encodeToString(file)}}"""
            val resp = client.delete("/api/backup/$type/delete", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =================== 数据库服务器管理 ===================

    /** 创建数据库服务器 */
    suspend fun createDatabaseServer(req: CreateDatabaseServerRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/database_server", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新数据库服务器 */
    suspend fun updateDatabaseServer(id: Long, req: UpdateDatabaseServerRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req.copy(id = id))
            val resp = client.put("/api/database_server/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("更新失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除数据库服务器 */
    suspend fun deleteDatabaseServer(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/database_server/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 同步数据库服务器用户 */
    suspend fun syncDatabaseServer(id: Long): Result<Unit> {
        return try {
            val resp = client.post("/api/database_server/$id/sync", "{}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 数据库用户 ===================

    /** 获取数据库用户列表 */
    suspend fun getDatabaseUsers(page: Int = 1, limit: Int = 20): Result<DatabaseUserListResponse> {
        return try {
            client.get(
                "/api/database_user",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建数据库用户 */
    suspend fun createDatabaseUser(req: CreateDatabaseUserRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/database_user", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除数据库用户 */
    suspend fun deleteDatabaseUser(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/database_user/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== WebHook ===================

    /** 获取 WebHook 列表 */
    suspend fun getWebHooks(page: Int = 1, limit: Int = 20): Result<WebHookListResponse> {
        return try {
            client.get(
                "/api/webhook",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建 WebHook */
    suspend fun createWebHook(req: CreateWebHookRequest): Result<WebHookItem> {
        return try {
            client.post("/api/webhook", json.encodeToString(req)).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新 WebHook（含状态切换） */
    suspend fun updateWebHook(id: Long, req: UpdateWebHookRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req.copy(id = id))
            val resp = client.put("/api/webhook/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除 WebHook */
    suspend fun deleteWebHook(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/webhook/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== SSH 主机 ===================

    /** 获取 SSH 主机列表 */
    suspend fun getSshHosts(page: Int = 1, limit: Int = 20): Result<SshHostListResponse> {
        return try {
            client.get(
                "/api/ssh",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建 SSH 主机 */
    suspend fun createSshHost(req: CreateSshRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/ssh", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除 SSH 主机 */
    suspend fun deleteSshHost(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/ssh/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新 SSH 主机 */
    suspend fun updateSshHost(id: Long, req: CreateSshRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(
                UpdateSshRequest(
                    id = id,
                    name = req.name,
                    host = req.host,
                    port = req.port,
                    auth_method = req.auth_method,
                    user = req.user,
                    password = req.password,
                    key = req.key,
                    passphrase = req.passphrase,
                    remark = req.remark
                )
            )
            val resp = client.put("/api/ssh/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("更新失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 备份存储 ===================

    /** 获取备份存储列表 */
    suspend fun getBackupStorages(page: Int = 1, limit: Int = 20): Result<BackupStorageListResponse> {
        return try {
            client.get(
                "/api/backup_storage",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建备份存储 */
    suspend fun createBackupStorage(req: CreateBackupStorageRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/backup_storage", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除备份存储 */
    suspend fun deleteBackupStorage(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/backup_storage/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新备份存储 */
    suspend fun updateBackupStorage(id: Long, req: CreateBackupStorageRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(
                UpdateBackupStorageRequest(id = id, type = req.type, name = req.name, info = req.info)
            )
            val resp = client.put("/api/backup_storage/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("更新失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 用户管理 ===================

    /** 获取用户列表 */
    suspend fun getUsers(page: Int = 1, limit: Int = 20): Result<UserListResponse> {
        return try {
            client.get(
                "/api/users",
                mapOf("page" to page.toString(), "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建用户 */
    suspend fun createUser(req: CreateUserRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/users", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除用户 */
    suspend fun deleteUser(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/users/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== Redis 键管理 ===================

    /** 获取 Redis 数据库数量 */
    suspend fun getRedisDatabases(serverId: Long): Result<Int> {
        return try {
            client.get("/api/database_redis/databases", mapOf("server_id" to serverId.toString()))
                .parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取 Redis 键列表 */
    suspend fun getRedisData(serverId: Long, db: Int = 0, search: String = "", page: Int = 1, limit: Int = 50): Result<RedisDataResponse> {
        return try {
            val params = mutableMapOf("server_id" to serverId.toString(), "db" to db.toString(), "page" to page.toString(), "limit" to limit.toString())
            if (search.isNotEmpty()) params["search"] = search
            client.get("/api/database_redis/data", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取单个 Redis 键详情 */
    suspend fun getRedisKey(serverId: Long, key: String, db: Int = 0): Result<RedisKVItem> {
        return try {
            client.get("/api/database_redis/key", mapOf("server_id" to serverId.toString(), "key" to key, "db" to db.toString()))
                .parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除 Redis 键 */
    suspend fun deleteRedisKey(serverId: Long, db: Int = 0, key: String): Result<Unit> {
        return try {
            val body = """{"server_id":$serverId,"db":$db,"key":${json.encodeToString(key)}}"""
            val resp = client.delete("/api/database_redis/key", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 清空 Redis DB */
    suspend fun clearRedisDB(serverId: Long, db: Int = 0): Result<Unit> {
        return try {
            val body = """{"server_id":$serverId,"db":$db}"""
            val resp = client.post("/api/database_redis/clear", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建/覆写 Redis 键 */
    suspend fun setRedisKey(req: CreateRedisKeyRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/database_redis/key", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 重命名 Redis 键 */
    suspend fun renameRedisKey(serverId: Long, db: Int = 0, oldKey: String, newKey: String): Result<Unit> {
        return try {
            val body = """{"server_id":$serverId,"db":$db,"old_key":${json.encodeToString(oldKey)},"new_key":${json.encodeToString(newKey)}}"""
            val resp = client.post("/api/database_redis/key/rename", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新 Redis 键 TTL */
    suspend fun setRedisKeyTTL(serverId: Long, db: Int = 0, key: String, ttl: Long): Result<Unit> {
        return try {
            val body = """{"server_id":$serverId,"db":$db,"key":${json.encodeToString(key)},"ttl":$ttl}"""
            val resp = client.post("/api/database_redis/key/ttl", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新计划任务 */
    suspend fun updateCronTask(id: Long, req: UpdateCronRequest): Result<Unit> {
        return try {
            val resp = client.put("/api/cron/$id", json.encodeToString(req.copy(id = id)))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新数据库用户密码/权限 */
    suspend fun updateDatabaseUser(id: Long, req: UpdateDatabaseUserRequest): Result<Unit> {
        return try {
            val resp = client.put("/api/database_user/$id", json.encodeToString(req.copy(id = id)))
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("更新失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建项目 */
    suspend fun createProject(req: CreateProjectRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/project", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = formatErrorBody(resp.bodyAsText()); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== ACME 账号 ===================

    /** 获取 ACME 账号列表 */
    suspend fun getAcmeAccounts(page: Int = 1, limit: Int = 50): Result<AcmeAccountListResponse> {
        return try {
            client.get("/api/cert/account", mapOf("page" to page.toString(), "limit" to limit.toString())).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建 ACME 账号 */
    suspend fun createAcmeAccount(req: CreateAcmeAccountRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/cert/account", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新 ACME 账号 */
    suspend fun updateAcmeAccount(id: Long, req: CreateAcmeAccountRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(
                UpdateAcmeAccountRequest(
                    id = id,
                    ca = req.ca,
                    email = req.email,
                    key_type = req.key_type,
                    kid = req.kid,
                    hmac_encoded = req.hmac_encoded
                )
            )
            val resp = client.put("/api/cert/account/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("更新失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除 ACME 账号 */
    suspend fun deleteAcmeAccount(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/cert/account/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== DNS 提供商 ===================

    /** 获取 DNS 提供商列表 */
    suspend fun getDnsProviders(page: Int = 1, limit: Int = 50): Result<DnsProviderListResponse> {
        return try {
            client.get("/api/cert/dns", mapOf("page" to page.toString(), "limit" to limit.toString())).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建 DNS 提供商 */
    suspend fun createDnsProvider(req: CreateDnsProviderRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/cert/dns", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("创建失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新 DNS 提供商 */
    suspend fun updateDnsProvider(id: Long, req: CreateDnsProviderRequest): Result<Unit> {
        return try {
            val body = json.encodeToString(
                UpdateDnsProviderRequest(id = id, type = req.type, name = req.name, data = req.data)
            )
            val resp = client.put("/api/cert/dns/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("更新失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 删除 DNS 提供商 */
    suspend fun deleteDnsProvider(id: Long): Result<Unit> {
        return try {
            val resp = client.delete("/api/cert/dns/$id")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新 Token */
    suspend fun updateToken(id: Long, ips: List<String>, expiredAt: Long): Result<Unit> {
        return try {
            val body = """{"ips":${json.encodeToString(ips)},"expired_at":$expiredAt}"""
            val resp = client.put("/api/user_tokens/$id", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("更新失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 用户资料修改 ===================

    /** 修改用户名 */
    suspend fun updateUsername(id: Long, username: String): Result<Unit> {
        return try {
            val body = """{"id":$id,"username":${json.encodeToString(username)}}"""
            val resp = client.post("/api/users/$id/username", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 修改密码 */
    suspend fun updatePassword(id: Long, password: String): Result<Unit> {
        return try {
            val body = """{"id":$id,"password":${json.encodeToString(password)}}"""
            val resp = client.post("/api/users/$id/password", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 修改邮箱 */
    suspend fun updateEmail(id: Long, email: String): Result<Unit> {
        return try {
            val body = """{"id":$id,"email":${json.encodeToString(email)}}"""
            val resp = client.post("/api/users/$id/email", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取用户 2FA 二维码信息 */
    suspend fun getUserTwoFa(id: Long): Result<TwoFaInfo> {
        return try {
            client.get("/api/users/$id/2fa").parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 启用/关闭用户 2FA（关闭时 secret/code 传空） */
    suspend fun setUserTwoFa(id: Long, secret: String, code: String): Result<Unit> {
        return try {
            val body = """{"secret":${json.encodeToString(secret)},"code":${json.encodeToString(code)}}"""
            val resp = client.post("/api/users/$id/2fa", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 网站统计 ===================

    /** 获取网站统计概览 */
    suspend fun getWebsiteStatOverview(start: String = "", end: String = ""): Result<WebsiteStatOverview> {
        return try {
            val params = mutableMapOf<String, String>()
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            client.get("/api/website/stat/overview", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取网站统计实时数据 */
    suspend fun getWebsiteStatRealtime(): Result<WebsiteStatRealtime> {
        return try {
            client.get("/api/website/stat/realtime").parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取网站维度统计 */
    suspend fun getWebsiteStatSites(start: String = "", end: String = "", sites: String = ""): Result<WebsiteStatSiteResponse> {
        return try {
            val params = mutableMapOf<String, String>()
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            client.get("/api/website/stat/sites", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取蜘蛛统计 */
    suspend fun getWebsiteStatSpiders(start: String = "", end: String = "", sites: String = ""): Result<WebsiteStatSpiderResponse> {
        return try {
            val params = mutableMapOf<String, String>()
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            client.get("/api/website/stat/spiders", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取客户端统计 */
    suspend fun getWebsiteStatClients(start: String = "", end: String = "", sites: String = ""): Result<WebsiteStatClientResponse> {
        return try {
            val params = mutableMapOf<String, String>()
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            client.get("/api/website/stat/clients", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取 IP 统计（分页） */
    suspend fun getWebsiteStatIps(
        start: String = "",
        end: String = "",
        sites: String = "",
        page: Int = 1,
        limit: Int = 20
    ): Result<WebsiteStatIpResponse> {
        return try {
            val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            client.get("/api/website/stat/ips", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取地域统计 */
    suspend fun getWebsiteStatGeos(
        start: String = "",
        end: String = "",
        sites: String = "",
        groupBy: String = "country",
        country: String = "",
        limit: Int = 100
    ): Result<WebsiteStatGeoResponse> {
        return try {
            val params = mutableMapOf("group_by" to groupBy, "limit" to limit.toString())
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            if (country.isNotEmpty()) params["country"] = country
            client.get("/api/website/stat/geos", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取 URI 统计（分页） */
    suspend fun getWebsiteStatUris(
        start: String = "",
        end: String = "",
        sites: String = "",
        page: Int = 1,
        limit: Int = 20
    ): Result<WebsiteStatUriResponse> {
        return try {
            val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            client.get("/api/website/stat/uris", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取慢 URI 统计（分页） */
    suspend fun getWebsiteStatSlowUris(
        start: String = "",
        end: String = "",
        sites: String = "",
        threshold: Int = 500,
        page: Int = 1,
        limit: Int = 20
    ): Result<WebsiteStatUriResponse> {
        return try {
            val params = mutableMapOf(
                "threshold" to threshold.toString(),
                "page" to page.toString(),
                "limit" to limit.toString()
            )
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            client.get("/api/website/stat/slow_uris", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取错误日志（分页） */
    suspend fun getWebsiteStatErrors(
        start: String = "",
        end: String = "",
        sites: String = "",
        status: Int = 0,
        page: Int = 1,
        limit: Int = 20
    ): Result<WebsiteStatErrorResponse> {
        return try {
            val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
            if (start.isNotEmpty()) params["start"] = start
            if (end.isNotEmpty()) params["end"] = end
            if (sites.isNotEmpty()) params["sites"] = sites
            if (status > 0) params["status"] = status.toString()
            client.get("/api/website/stat/errors", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取网站统计设置 */
    suspend fun getWebsiteStatSetting(): Result<WebsiteStatSetting> {
        return try {
            client.get("/api/website/stat/setting").parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 保存网站统计设置 */
    suspend fun setWebsiteStatSetting(req: WebsiteStatSetting): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/website/stat/setting", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 清空网站统计数据 */
    suspend fun clearWebsiteStats(): Result<Unit> {
        return try {
            val resp = client.post("/api/website/stat/clear", "{}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 防火墙扫描审计 ===================

    /** 获取扫描设置 */
    suspend fun getFirewallScanSetting(): Result<FirewallScanSetting> {
        return try {
            client.get("/api/firewall/scan/setting").parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 保存扫描设置 */
    suspend fun setFirewallScanSetting(req: FirewallScanSetting): Result<Unit> {
        return try {
            val body = json.encodeToString(req)
            val resp = client.post("/api/firewall/scan/setting", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else { val err = resp.bodyAsText(); Result.failure(Exception("失败: $err")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取扫描汇总 */
    suspend fun getFirewallScanSummary(start: String, end: String): Result<FirewallScanSummary> {
        return try {
            client.get("/api/firewall/scan/summary", mapOf("start" to start, "end" to end)).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取 Top 攻击 IP */
    suspend fun getFirewallScanTopIps(start: String, end: String, limit: Int = 10): Result<List<ScanTopItem>> {
        return try {
            client.get(
                "/api/firewall/scan/top_ips",
                mapOf("start" to start, "end" to end, "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取 Top 被扫描端口 */
    suspend fun getFirewallScanTopPorts(start: String, end: String, limit: Int = 10): Result<List<ScanTopItem>> {
        return try {
            client.get(
                "/api/firewall/scan/top_ports",
                mapOf("start" to start, "end" to end, "limit" to limit.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取扫描事件列表 */
    suspend fun getFirewallScanEvents(
        start: String, end: String, page: Int = 1, limit: Int = 20,
        sourceIp: String = "", port: String = ""
    ): Result<ScanEventListResponse> {
        return try {
            val params = mutableMapOf(
                "start" to start, "end" to end,
                "page" to page.toString(), "limit" to limit.toString()
            )
            if (sourceIp.isNotEmpty()) params["source_ip"] = sourceIp
            if (port.isNotEmpty()) params["port"] = port
            client.get("/api/firewall/scan/events", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 清空扫描数据 */
    suspend fun clearFirewallScanData(): Result<Unit> {
        return try {
            val resp = client.post("/api/firewall/scan/clear", "{}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 进程管理 ===================

    suspend fun getProcessList(
        page: Int = 1,
        limit: Int = 20,
        sort: String = "cpu",
        order: String = "desc",
        keyword: String = ""
    ): Result<ProcessListResponse> {
        return try {
            val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString(),
                "sort" to sort, "order" to order)
            if (keyword.isNotEmpty()) params["keyword"] = keyword
            client.get("/api/process", params).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun killProcess(pid: Int): Result<Unit> {
        return try {
            val resp = client.post("/api/process/kill", """{"pid":$pid}""")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 防火墙 IP 规则 ===================

    suspend fun getIpRules(page: Int = 1, limit: Int = 100): Result<IpRuleListResponse> {
        return try {
            client.get("/api/firewall/ip_rule", mapOf("page" to page.toString(), "limit" to limit.toString()))
                .parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addIpRule(req: CreateIpRuleRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/firewall/ip_rule", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteIpRule(req: CreateIpRuleRequest): Result<Unit> {
        return try {
            val resp = client.delete("/api/firewall/ip_rule", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 防火墙端口转发 ===================

    suspend fun getForwardRules(page: Int = 1, limit: Int = 100): Result<ForwardRuleListResponse> {
        return try {
            client.get("/api/firewall/forward", mapOf("page" to page.toString(), "limit" to limit.toString()))
                .parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun addForwardRule(req: CreateForwardRuleRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/firewall/forward", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteForwardRule(req: CreateForwardRuleRequest): Result<Unit> {
        return try {
            val resp = client.delete("/api/firewall/forward", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${resp.status.value}"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 防火墙端口规则添加 ===================

    /** 添加防火墙端口规则 */
    suspend fun addFirewallRule(req: CreateFirewallRuleRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/firewall/rule", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 备份恢复 ===================

    /** 恢复备份文件到目标 */
    suspend fun restoreBackup(type: String, file: String, target: String): Result<Unit> {
        return try {
            val body = """{"file":${json.encodeToString(file)},"target":${json.encodeToString(target)}}"""
            val resp = client.post("/api/backup/$type/restore", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== 计划任务创建 ===================

    suspend fun createCronTask(req: CreateCronRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/cron", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建证书（ACME/自签） */
    suspend fun createCert(req: CreateCertRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/cert/cert", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 上传证书（PEM格式） */
    suspend fun uploadCert(req: UploadCertRequest): Result<Unit> {
        return try {
            val resp = client.post("/api/cert/cert/upload", json.encodeToString(req))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** ACME 自动签发 */
    suspend fun obtainAutoSigned(id: Long): Result<Unit> {
        return try {
            val resp = client.post("/api/cert/cert/$id/obtain_auto", "{}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 自签名签发 */
    suspend fun obtainSelfSigned(id: Long): Result<Unit> {
        return try {
            val resp = client.post("/api/cert/cert/$id/obtain_self_signed", "{}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 创建备份 */
    suspend fun createBackup(type: String, target: String = "", storage: Long = 0): Result<Unit> {
        return try {
            val body = json.encodeToString(CreateBackupRequest(target, storage))
            val resp = client.post("/api/backup/$type", body)
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取监控历史数据 */
    suspend fun getMonitorHistory(start: Long, end: Long): Result<MonitorHistoryResponse> {
        return try {
            client.get(
                "/api/monitor/list",
                mapOf("start" to start.toString(), "end" to end.toString())
            ).parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 获取监控设置 */
    suspend fun getMonitorSetting(): Result<MonitorSetting> {
        return try {
            client.get("/api/monitor/setting").parseData()
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 更新监控设置 */
    suspend fun setMonitorSetting(setting: MonitorSetting): Result<Unit> {
        return try {
            val resp = client.post("/api/monitor/setting", json.encodeToString(setting))
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 清空监控历史数据 */
    suspend fun clearMonitorData(): Result<Unit> {
        return try {
            val resp = client.post("/api/monitor/clear", "{}")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    /** 发送信号到进程 */
    suspend fun sendProcessSignal(pid: Int, signal: Int): Result<Unit> {
        return try {
            val resp = client.post("/api/process/signal", """{"pid":$pid,"signal":$signal}""")
            if (resp.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception(resp.bodyAsText()))
        } catch (e: Exception) { Result.failure(e) }
    }

    // =================== Session 入口 ===================

    /** 初始化安全入口，Session 模式 + entrance 非空时调用 */
    suspend fun initEntrance(): Result<Unit> {
        return client.initEntrance()
    }

    // =================== WebSocket 终端 ===================

    /**
     * 启动 PTY 会话（面板本机 bash）
     * @param inputChannel 用户输入通道
     * @param onOutput 输出回调（调用方负责 ANSI 处理）
     */
    suspend fun startPtySession(
        inputChannel: ReceiveChannel<String>,
        initialPath: String = "/",
        onOutput: (String) -> Unit
    ): Result<Unit> {
        return try {
            client.connectWs("/api/ws/pty") {
                val shellPath = initialPath.trim().ifBlank { "/" }.replace("'", "'\"'\"'")
                send(Frame.Text("cd '$shellPath' && exec bash -l\n"))
                val senderJob = launch {
                    for (cmd in inputChannel) {
                        send(Frame.Text(cmd))
                    }
                }
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> onOutput(frame.readText())
                        is Frame.Binary -> onOutput(frame.data.decodeToString())
                        else -> {}
                    }
                }
                senderJob.cancel()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 启动 SSH 会话（通过已保存的 SSH 主机配置）
     * @param sshId SSH 主机 ID
     */
    suspend fun startSshSession(
        sshId: Long,
        inputChannel: ReceiveChannel<String>,
        onOutput: (String) -> Unit
    ): Result<Unit> {
        return try {
            client.connectWs("/api/ws/ssh", mapOf("id" to sshId.toString())) {
                val senderJob = launch {
                    for (cmd in inputChannel) {
                        send(Frame.Text(cmd))
                    }
                }
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> onOutput(frame.readText())
                        is Frame.Binary -> onOutput(frame.data.decodeToString())
                        else -> {}
                    }
                }
                senderJob.cancel()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() = client.close()
}
