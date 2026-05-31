package com.acepanel.app.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.acepanel.app.data.ApiResponse
import com.acepanel.app.data.CaptchaResponse
import com.acepanel.app.data.LoginRequest
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.feedback.FeedbackCenter

/**
 * 底层 HTTP 客户端，处理 Token HMAC 签名和 Session Cookie 鉴权
 * 使用平台特定的 createHttpClient() 以支持自签名证书（Android OkHttp）
 */
class ApiClient(val config: PanelConfig) {

    /** Session 模式下，登录后存储的 Cookie，自动应用于后续请求 */
    var storedSessionCookie: String? = SessionCookieStore.get(config.id) ?: PanelRepository.getSessionCookie(config.id)

    private val httpClient: HttpClient = sharedHttpClient

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    /**
     * 构建请求 URL（Token 模式带入口前缀）
     */
    fun buildUrl(path: String): String {
        return config.apiUrl(path)
    }

    /**
     * 合并 Set-Cookie 响应头到现有 Cookie 字符串
     * - 解析时过滤掉 Path/Domain/HttpOnly/SameSite/Max-Age/Expires/Secure 等属性
     * - 以 "name1=val1; name2=val2" 格式存储，发送 Cookie 请求头时格式正确
     */
    private fun mergeSetCookies(existing: String?, setCookieHeaders: List<String>): String {
        val cookieMap = LinkedHashMap<String, String>()
        // 解析现有 Cookie 字符串（可能包含旧格式的属性，需要过滤）
        existing?.split(";")?.forEach { part ->
            val s = part.trim()
            val i = s.indexOf('=')
            val name = if (i > 0) s.substring(0, i).trim() else s
            if (name.isNotEmpty() && name.lowercase() !in COOKIE_ATTR_NAMES) {
                cookieMap[name] = if (i > 0) s.substring(i + 1).trim() else ""
            }
        }
        // 应用新的 Set-Cookie（只取首段 name=value，去掉属性部分）
        setCookieHeaders.forEach { setCookie ->
            val nameValue = setCookie.substringBefore(";").trim()
            val i = nameValue.indexOf('=')
            if (i > 0) {
                val name = nameValue.substring(0, i).trim()
                val value = nameValue.substring(i + 1).trim()
                if (name.lowercase() !in COOKIE_ATTR_NAMES) {
                    cookieMap[name] = value
                }
            }
        }
        return cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    companion object {
        /**
         * 全局共享 HttpClient，避免不同页面反复创建连接池导致频繁重连。
         * 实际请求鉴权仍由每次请求头与 PanelConfig 控制。
         */
        private val sharedHttpClient: HttpClient by lazy { createHttpClient() }

        /** Cookie 属性名，不应作为 Cookie name 存储 */
        private val COOKIE_ATTR_NAMES = setOf(
            "path", "domain", "max-age", "expires", "httponly", "samesite", "secure"
        )

        /** 最多跟随重定向次数，避免循环重定向 */
        private const val MAX_REDIRECT_FOLLOWS = 8

        /** 使用 POST 取数据的接口，不应触发全局“执行中/完成”反馈 */
        private val silentWritePaths = setOf(
            "/api/home/current"
        )

        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    }

    private fun requestUserAgent(): String {
        return config.userAgent.trim().ifBlank { DEFAULT_USER_AGENT }
    }

    /**
     * 计算 HMAC-SHA256 签名并写入请求头
     * @param path  签名用路径（始终是 /api/... 不含入口前缀）
     * @param query URL 编码后的查询串（按 key 排序）
     * @param bodyBytes 请求体原始字节
     */
    private fun HttpRequestBuilder.addHmacAuth(
        method: String,
        path: String,
        query: String,
        bodyBytes: ByteArray
    ) {
        val timestamp = currentTimeSeconds().toString()
        val bodyHash = sha256Hex(bodyBytes)
        val canonicalRequest = "$method\n$path\n$query\n$bodyHash"
        val canonicalHash = sha256Hex(canonicalRequest.encodeToByteArray())
        val stringToSign = "HMAC-SHA256\n$timestamp\n$canonicalHash"
        val signature = hmacSha256Hex(
            config.tokenSecret.encodeToByteArray(),
            stringToSign.encodeToByteArray()
        )
        header("Authorization", "HMAC-SHA256 Credential=${config.tokenId}, Signature=$signature")
        header("X-Timestamp", timestamp)
    }

    /**
     * 将 Map 编码为 URL 查询串（按 key 字典序排序）
     */
    private fun encodeQuery(params: Map<String, String>): String {
        return params.entries
            .sortedBy { it.key }
            .joinToString("&") { (k, v) -> "${k.encodeURLParameter()}=${v.encodeURLParameter()}" }
    }

    private fun HttpStatusCode.isRedirectStatus(): Boolean {
        return this == HttpStatusCode.MovedPermanently ||
            this == HttpStatusCode.Found ||
            this == HttpStatusCode.SeeOther ||
            this == HttpStatusCode.TemporaryRedirect ||
            this == HttpStatusCode.PermanentRedirect
    }

    private fun resolveRedirectUrl(currentUrl: String, location: String): String {
        val target = location.trim()
        if (target.startsWith("http://") || target.startsWith("https://")) return target

        val base = Url(currentUrl)
        val scheme = base.protocol.name
        val authority = buildString {
            append(base.host)
            if (base.port != base.protocol.defaultPort) append(":${base.port}")
        }

        return when {
            target.startsWith("//") -> "$scheme:$target"
            target.startsWith("/") -> "$scheme://$authority$target"
            target.startsWith("?") -> "$scheme://$authority${base.encodedPath}$target"
            target.startsWith("#") -> {
                val query = if (base.encodedQuery.isNotEmpty()) "?${base.encodedQuery}" else ""
                "$scheme://$authority${base.encodedPath}$query$target"
            }
            else -> {
                val baseDir = base.encodedPath.substringBeforeLast("/", "")
                val prefix = if (baseDir.isEmpty()) "/" else "$baseDir/"
                "$scheme://$authority$prefix${target.trimStart('/')}"
            }
        }
    }

    private fun resolvePathForSign(currentUrl: String, fallbackPath: String): String {
        val path = runCatching { Url(currentUrl).encodedPath }.getOrNull().orEmpty()
        if (path.isBlank()) return fallbackPath
        if (path == "/api" || path.startsWith("/api/")) return path

        val idx = path.indexOf("/api")
        if (idx >= 0) {
            val boundaryIdx = idx + 4
            if (boundaryIdx == path.length || path[boundaryIdx] == '/') {
                return path.substring(idx)
            }
        }
        return fallbackPath
    }

    private fun resolveQueryForSign(currentUrl: String): String {
        val url = runCatching { Url(currentUrl) }.getOrNull() ?: return ""
        val pairs = url.parameters.entries()
            .flatMap { (k, values) -> values.map { v -> k to v } }
            .sortedBy { it.first }
        if (pairs.isEmpty()) return ""
        return pairs.joinToString("&") { (k, v) ->
            "${k.encodeURLParameter()}=${v.encodeURLParameter()}"
        }
    }

    private fun mergeSessionCookiesFrom(resp: HttpResponse): Boolean {
        if (config.authMode != "session") return false
        val newCookies = resp.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()
        if (newCookies.isNotEmpty()) {
            storedSessionCookie = mergeSetCookies(storedSessionCookie, newCookies)
            storedSessionCookie?.takeIf { it.isNotBlank() }?.let {
                persistSessionCookie(it)
            }
            return !storedSessionCookie.isNullOrBlank()
        }
        return false
    }

    private fun persistSessionCookie(cookie: String) {
        SessionCookieStore.set(config.id, cookie)
        PanelRepository.saveSessionCookie(config.id, cookie)
    }

    private data class FeedbackLabels(
        val working: String,
        val success: String,
        val failure: String,
        val detail: String? = null
    )

    private fun feedbackLabels(method: HttpMethod, path: String): FeedbackLabels? {
        if (method == HttpMethod.Get) return null

        val normalizedPath = path.substringBefore("?")
        if (normalizedPath in silentWritePaths) return null

        val target = feedbackTarget(normalizedPath)
        val action = feedbackAction(method, normalizedPath)
        return FeedbackLabels(
            working = "正在${action.present}$target",
            success = "${target}${action.done}",
            failure = "${target}${action.failed}",
            detail = config.name.ifBlank { "${config.host}:${config.port}" }
        )
    }

    private data class FeedbackAction(
        val present: String,
        val done: String,
        val failed: String
    )

    private fun feedbackAction(method: HttpMethod, path: String): FeedbackAction {
        return when {
            path == "/api/user/login" -> FeedbackAction("登录", "登录成功", "登录失败")
            path == "/api/user/logout" -> FeedbackAction("退出登录", "已退出登录", "退出登录失败")
            path.contains("/install") -> FeedbackAction("安装", "安装任务已提交", "安装失败")
            path.contains("/uninstall") -> FeedbackAction("卸载", "卸载任务已提交", "卸载失败")
            path.contains("/restart") -> FeedbackAction("重启", "已重启", "重启失败")
            path.contains("/start") -> FeedbackAction("启动", "已启动", "启动失败")
            path.contains("/stop") -> FeedbackAction("停止", "已停止", "停止失败")
            path.contains("/enable") -> FeedbackAction("启用", "已启用", "启用失败")
            path.contains("/disable") -> FeedbackAction("禁用", "已禁用", "禁用失败")
            path.contains("/renew") -> FeedbackAction("续签", "续签任务已提交", "续签失败")
            path.contains("/sync") -> FeedbackAction("同步", "已同步", "同步失败")
            path.contains("/restore") -> FeedbackAction("恢复", "恢复任务已提交", "恢复失败")
            path.contains("/clear") -> FeedbackAction("清理", "已清理", "清理失败")
            path.contains("/kill") -> FeedbackAction("结束", "已结束", "结束失败")
            path.contains("/signal") -> FeedbackAction("发送信号到", "信号已发送", "信号发送失败")
            path.contains("/status") -> FeedbackAction("切换", "状态已更新", "状态更新失败")
            path.contains("/save") -> FeedbackAction("保存", "已保存", "保存失败")
            path.contains("/ttl") -> FeedbackAction("更新", "已更新", "更新失败")
            path.contains("/rename") -> FeedbackAction("重命名", "已重命名", "重命名失败")
            method == HttpMethod.Delete || path.contains("/delete") -> FeedbackAction("删除", "已删除", "删除失败")
            method == HttpMethod.Put -> FeedbackAction("更新", "已更新", "更新失败")
            method == HttpMethod.Post -> FeedbackAction("提交", "已提交", "提交失败")
            else -> FeedbackAction("执行", "已完成", "执行失败")
        }
    }

    private fun feedbackTarget(path: String): String {
        return when {
            path.startsWith("/api/user/login") || path.startsWith("/api/user/logout") -> ""
            path.startsWith("/api/website/stat") -> "网站统计"
            path.startsWith("/api/firewall/scan") -> "防火墙扫描"
            path.startsWith("/api/website") -> "网站"
            path.startsWith("/api/database_redis") -> "Redis"
            path.startsWith("/api/database_server") -> "数据库服务器"
            path.startsWith("/api/database_user") -> "数据库用户"
            path.startsWith("/api/database") -> "数据库"
            path.startsWith("/api/user_tokens") -> "Token"
            path.startsWith("/api/firewall/ip_rule") -> "IP 规则"
            path.startsWith("/api/firewall/forward") -> "转发规则"
            path.startsWith("/api/firewall/rule") -> "防火墙规则"
            path.startsWith("/api/firewall") -> "防火墙"
            path.startsWith("/api/cron") -> "计划任务"
            path.startsWith("/api/setting/memo") -> "备注"
            path.startsWith("/api/setting") -> "面板设置"
            path.startsWith("/api/app") -> "应用"
            path.startsWith("/api/task") -> "任务"
            path.startsWith("/api/project") -> "项目"
            path.startsWith("/api/systemctl") -> "服务"
            path.startsWith("/api/cert/account") -> "ACME 账号"
            path.startsWith("/api/cert/dns") -> "DNS 提供商"
            path.startsWith("/api/cert/cert") -> "证书"
            path.startsWith("/api/file") -> "文件"
            path.startsWith("/api/backup_storage") -> "备份存储"
            path.startsWith("/api/backup") -> "备份"
            path.startsWith("/api/webhook") -> "WebHook"
            path.startsWith("/api/ssh") -> "SSH 主机"
            path.startsWith("/api/users") -> "用户"
            path.startsWith("/api/process") -> "进程"
            path.startsWith("/api/monitor") -> "监控数据"
            else -> "操作"
        }
    }

    private suspend fun requestWithFeedback(
        method: HttpMethod,
        pathForSign: String,
        queryForSign: String,
        bodyJson: String?,
        queryParams: Map<String, String>,
        sessionCookie: String?,
        bodyBytesOverride: ByteArray? = null,
        bodyContentType: ContentType = ContentType.Application.Json
    ): HttpResponse {
        val labels = feedbackLabels(method, pathForSign)
        val operationId = labels?.let { FeedbackCenter.begin(it.working, it.detail) }

        return try {
            val resp = requestWithRedirect(
                method = method,
                pathForSign = pathForSign,
                queryForSign = queryForSign,
                bodyJson = bodyJson,
                queryParams = queryParams,
                sessionCookie = sessionCookie,
                bodyBytesOverride = bodyBytesOverride,
                bodyContentType = bodyContentType
            )
            if (labels != null) {
                if (resp.status.isSuccess()) {
                    FeedbackCenter.succeed(operationId, labels.success, labels.detail)
                } else {
                    val statusText = "HTTP ${resp.status.value}${resp.status.description.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}"
                    FeedbackCenter.fail(operationId, labels.failure, statusText)
                }
            }
            resp
        } catch (e: Exception) {
            if (labels != null) {
                FeedbackCenter.fail(operationId, labels.failure, e.message ?: "请求失败")
            }
            throw e
        }
    }

    private suspend fun requestWithRedirect(
        method: HttpMethod,
        pathForSign: String,
        queryForSign: String,
        bodyJson: String?,
        queryParams: Map<String, String>,
        sessionCookie: String?,
        bodyBytesOverride: ByteArray? = null,
        bodyContentType: ContentType = ContentType.Application.Json,
        retryEntranceDenied: Boolean = true,
        retrySessionExpired: Boolean = true
    ): HttpResponse {
        val bodyBytes = bodyBytesOverride ?: (bodyJson ?: "").encodeToByteArray()
        var currentUrl = buildUrl(pathForSign)
        var redirectCount = 0

        while (true) {
            val effectiveCookie = sessionCookie ?: storedSessionCookie
            val resp = httpClient.request(currentUrl) {
                this.method = method
                header(HttpHeaders.UserAgent, requestUserAgent())

                if (method == HttpMethod.Get && !currentUrl.contains("?")) {
                    queryParams.forEach { (k, v) -> parameter(k, v) }
                }

                if (bodyJson != null) {
                    if (bodyJson.isNotEmpty() || method == HttpMethod.Post || method == HttpMethod.Put) {
                        contentType(bodyContentType)
                    }
                    if (bodyJson.isNotEmpty() || method == HttpMethod.Post || method == HttpMethod.Put) {
                        if (bodyBytesOverride != null) {
                            setBody(bodyBytesOverride)
                        } else {
                            setBody(bodyJson)
                        }
                    }
                }

                if (config.authMode == "token") {
                    val signedPath = resolvePathForSign(currentUrl, pathForSign)
                    val query = if (method == HttpMethod.Get) {
                        if (currentUrl.contains("?")) resolveQueryForSign(currentUrl) else queryForSign
                    } else ""
                    addHmacAuth(method.value, signedPath, query, bodyBytes)
                } else if (effectiveCookie != null) {
                    header(HttpHeaders.Cookie, effectiveCookie)
                }
            }

            mergeSessionCookiesFrom(resp)

            val location = resp.headers[HttpHeaders.Location]
            if (!resp.status.isRedirectStatus() || location.isNullOrBlank() || redirectCount >= MAX_REDIRECT_FOLLOWS) {
                if (
                    resp.status.value == 418 &&
                    config.authMode == "session" &&
                    retryEntranceDenied &&
                    initEntrance().isSuccess
                ) {
                    return requestWithRedirect(
                        method = method,
                        pathForSign = pathForSign,
                        queryForSign = queryForSign,
                        bodyJson = bodyJson,
                        queryParams = queryParams,
                        sessionCookie = sessionCookie,
                        bodyBytesOverride = bodyBytesOverride,
                        bodyContentType = bodyContentType,
                        retryEntranceDenied = false,
                        retrySessionExpired = retrySessionExpired
                    )
                }
                if (
                    resp.status == HttpStatusCode.Unauthorized &&
                    config.authMode == "session" &&
                    retrySessionExpired &&
                    sessionCookie == null &&
                    pathForSign != "/api/user/login" &&
                    autoRelogin().isSuccess
                ) {
                    return requestWithRedirect(
                        method = method,
                        pathForSign = pathForSign,
                        queryForSign = queryForSign,
                        bodyJson = bodyJson,
                        queryParams = queryParams,
                        sessionCookie = null,
                        bodyBytesOverride = bodyBytesOverride,
                        bodyContentType = bodyContentType,
                        retryEntranceDenied = retryEntranceDenied,
                        retrySessionExpired = false
                    )
                }
                return resp
            }

            currentUrl = resolveRedirectUrl(currentUrl, location)
            redirectCount++
        }
    }

    private suspend fun autoRelogin(): Result<Unit> {
        if (config.authMode != "session") return Result.success(Unit)
        val username = config.sessionUsername.trim()
        val password = config.sessionPassword
        if (username.isEmpty() || password.isEmpty()) {
            return Result.failure(Exception("未保存账号密码，无法自动重新登录"))
        }

        return try {
            initEntrance().getOrThrow()

            val captchaResp = httpClient.get(buildUrl("/api/user/captcha")) {
                header(HttpHeaders.UserAgent, requestUserAgent())
                storedSessionCookie?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Cookie, it) }
            }
            mergeSessionCookiesFrom(captchaResp)
            if (captchaResp.status.isSuccess()) {
                val captchaText = captchaResp.bodyAsText()
                val captcha = runCatching {
                    json.decodeFromString<ApiResponse<CaptchaResponse>>(captchaText).data
                }.getOrNull()
                if (captcha?.required == true) {
                    return Result.failure(Exception("面板启用了图形验证码，无法自动重新登录"))
                }
            }

            val twoFaResp = httpClient.get(buildUrl("/api/user/is_2fa")) {
                header(HttpHeaders.UserAgent, requestUserAgent())
                parameter("username", username)
                storedSessionCookie?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Cookie, it) }
            }
            mergeSessionCookiesFrom(twoFaResp)
            if (twoFaResp.status.isSuccess()) {
                val twoFaText = twoFaResp.bodyAsText()
                val twoFaEnabled = runCatching {
                    json.decodeFromString<ApiResponse<Boolean>>(twoFaText).data == true
                }.getOrDefault(false)
                if (twoFaEnabled) {
                    return Result.failure(Exception("面板启用了 2FA，无法自动重新登录"))
                }
            }

            val keyResp = httpClient.get(buildUrl("/api/user/key")) {
                header(HttpHeaders.UserAgent, requestUserAgent())
                storedSessionCookie?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Cookie, it) }
            }
            mergeSessionCookiesFrom(keyResp)
            if (!keyResp.status.isSuccess()) {
                return Result.failure(Exception("获取登录公钥失败：HTTP ${keyResp.status.value}"))
            }
            val publicKey = json.decodeFromString<ApiResponse<String>>(keyResp.bodyAsText()).data
                ?: return Result.failure(Exception("服务器未返回登录公钥"))

            val req = LoginRequest(
                username = rsaEncryptOaepSha512(publicKey, username.encodeToByteArray()),
                password = rsaEncryptOaepSha512(publicKey, password.encodeToByteArray())
            )
            val body = json.encodeToString(req)
            val loginResp = requestWithRedirect(
                method = HttpMethod.Post,
                pathForSign = "/api/user/login",
                queryForSign = "",
                bodyJson = body,
                queryParams = emptyMap(),
                sessionCookie = null,
                retryEntranceDenied = true,
                retrySessionExpired = false
            )
            if (loginResp.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("自动重新登录失败：HTTP ${loginResp.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        sessionCookie: String? = null
    ): HttpResponse {
        val query = encodeQuery(queryParams)
        return requestWithRedirect(
            method = HttpMethod.Get,
            pathForSign = path,
            queryForSign = query,
            bodyJson = null,
            queryParams = queryParams,
            sessionCookie = sessionCookie
        )
    }

    suspend fun post(
        path: String,
        bodyJson: String = "{}",
        sessionCookie: String? = null
    ): HttpResponse {
        return requestWithFeedback(
            method = HttpMethod.Post,
            pathForSign = path,
            queryForSign = "",
            bodyJson = bodyJson,
            queryParams = emptyMap(),
            sessionCookie = sessionCookie
        )
    }

    suspend fun postBytes(
        path: String,
        bytes: ByteArray,
        contentType: ContentType,
        sessionCookie: String? = null
    ): HttpResponse {
        return requestWithFeedback(
            method = HttpMethod.Post,
            pathForSign = path,
            queryForSign = "",
            bodyJson = "",
            queryParams = emptyMap(),
            sessionCookie = sessionCookie,
            bodyBytesOverride = bytes,
            bodyContentType = contentType
        )
    }

    suspend fun put(
        path: String,
        bodyJson: String = "{}",
        sessionCookie: String? = null
    ): HttpResponse {
        return requestWithFeedback(
            method = HttpMethod.Put,
            pathForSign = path,
            queryForSign = "",
            bodyJson = bodyJson,
            queryParams = emptyMap(),
            sessionCookie = sessionCookie
        )
    }

    suspend fun delete(
        path: String,
        bodyJson: String = "",
        sessionCookie: String? = null
    ): HttpResponse {
        return requestWithFeedback(
            method = HttpMethod.Delete,
            pathForSign = path,
            queryForSign = "",
            bodyJson = bodyJson,
            queryParams = emptyMap(),
            sessionCookie = sessionCookie
        )
    }

    /**
     * 初始化安全入口，GET /{entrance} 以获取 verify_entrance Cookie
     * Session 模式下即使入口为空也访问根路径，用于服务端入口为 / 时建立 verify_entrance。
     */
    suspend fun initEntrance(): Result<Unit> {
        if (config.authMode != "session") return Result.success(Unit)
        return try {
            var currentUrl = config.entranceUrl()
            var redirectCount = 0
            var lastStatus = HttpStatusCode.OK
            var receivedEntranceCookie = false
            while (true) {
                val resp = httpClient.get(currentUrl) {
                    header(HttpHeaders.UserAgent, requestUserAgent())
                    storedSessionCookie?.takeIf { it.isNotBlank() }?.let {
                        header(HttpHeaders.Cookie, it)
                    }
                }
                lastStatus = resp.status
                if (lastStatus.value != 418) {
                    receivedEntranceCookie = mergeSessionCookiesFrom(resp) || receivedEntranceCookie
                }
                val location = resp.headers[HttpHeaders.Location]
                val shouldFollowToSecureUrl = (
                    resp.status == HttpStatusCode.MovedPermanently ||
                        resp.status == HttpStatusCode.TemporaryRedirect ||
                        resp.status == HttpStatusCode.PermanentRedirect
                    ) && !location.isNullOrBlank() && redirectCount < MAX_REDIRECT_FOLLOWS
                if (!shouldFollowToSecureUrl) break
                currentUrl = resolveRedirectUrl(currentUrl, location)
                redirectCount++
            }
            if (lastStatus.value == 418) {
                Result.failure(Exception("安全入口验证失败：HTTP ${lastStatus.value} ${lastStatus.description}"))
            } else if (lastStatus.isSuccess() || lastStatus.isRedirectStatus() || receivedEntranceCookie) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("安全入口验证失败：HTTP ${lastStatus.value} ${lastStatus.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 建立 WebSocket 连接（仅 Session Cookie 模式有效）
     * WS URL 不含入口前缀
     */
    suspend fun connectWs(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        block: suspend DefaultClientWebSocketSession.() -> Unit
    ) {
        if (config.authMode == "session") {
            initEntrance().getOrElse { throw it }
        }
        val wsScheme = if (config.scheme == "https") "wss" else "ws"
        val query = if (queryParams.isEmpty()) "" else "?" + queryParams.entries
            .sortedBy { it.key }
            .joinToString("&") { (k, v) -> "${k.encodeURLParameter()}=${v.encodeURLParameter()}" }
        val url = "$wsScheme://${config.host}:${config.port}$path$query"
        val cookie = storedSessionCookie
        httpClient.webSocket(
            urlString = url,
            request = {
                header(HttpHeaders.UserAgent, requestUserAgent())
                if (cookie != null) header(HttpHeaders.Cookie, cookie)
            },
            block = block
        )
    }

    /** 共享客户端不在单个服务生命周期内关闭 */
    fun close() {}
}
