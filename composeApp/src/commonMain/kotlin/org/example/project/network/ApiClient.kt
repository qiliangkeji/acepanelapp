package org.example.project.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.example.project.data.PanelConfig

/**
 * 底层 HTTP 客户端，处理 Token HMAC 签名和 Session Cookie 鉴权
 * 使用平台特定的 createHttpClient() 以支持自签名证书（Android OkHttp）
 */
class ApiClient(val config: PanelConfig) {

    /** Session 模式下，登录后存储的 Cookie，自动应用于后续请求 */
    var storedSessionCookie: String? = SessionCookieStore.get(config.id)

    private val httpClient: HttpClient = sharedHttpClient

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
        val canonicalHash = sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))
        val stringToSign = "HMAC-SHA256\n$timestamp\n$canonicalHash"
        val signature = hmacSha256Hex(
            config.tokenSecret.toByteArray(Charsets.UTF_8),
            stringToSign.toByteArray(Charsets.UTF_8)
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

    private fun mergeSessionCookiesFrom(resp: HttpResponse) {
        if (config.authMode != "session") return
        val newCookies = resp.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()
        if (newCookies.isNotEmpty()) {
            storedSessionCookie = mergeSetCookies(storedSessionCookie, newCookies)
        }
    }

    private suspend fun requestWithRedirect(
        method: HttpMethod,
        pathForSign: String,
        queryForSign: String,
        bodyJson: String?,
        queryParams: Map<String, String>,
        sessionCookie: String?
    ): HttpResponse {
        val bodyBytes = (bodyJson ?: "").toByteArray(Charsets.UTF_8)
        var currentUrl = buildUrl(pathForSign)
        var redirectCount = 0

        while (true) {
            val effectiveCookie = sessionCookie ?: storedSessionCookie
            val resp = httpClient.request(currentUrl) {
                this.method = method

                if (method == HttpMethod.Get && !currentUrl.contains("?")) {
                    queryParams.forEach { (k, v) -> parameter(k, v) }
                }

                if (bodyJson != null) {
                    if (bodyJson.isNotEmpty() || method == HttpMethod.Post || method == HttpMethod.Put) {
                        contentType(ContentType.Application.Json)
                    }
                    if (bodyJson.isNotEmpty() || method == HttpMethod.Post || method == HttpMethod.Put) {
                        setBody(bodyJson)
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
                return resp
            }

            currentUrl = resolveRedirectUrl(currentUrl, location)
            redirectCount++
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
        return requestWithRedirect(
            method = HttpMethod.Post,
            pathForSign = path,
            queryForSign = "",
            bodyJson = bodyJson,
            queryParams = emptyMap(),
            sessionCookie = sessionCookie
        )
    }

    suspend fun put(
        path: String,
        bodyJson: String = "{}",
        sessionCookie: String? = null
    ): HttpResponse {
        return requestWithRedirect(
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
        return requestWithRedirect(
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
     * 仅 Session 模式且 entrance 非空时有效
     */
    suspend fun initEntrance() {
        if (config.entrance.isEmpty()) return
        try {
            var currentUrl = config.entranceUrl()
            var redirectCount = 0
            while (true) {
                val resp = httpClient.get(currentUrl)
                mergeSessionCookiesFrom(resp)
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
        } catch (_: Exception) {
            // 非致命错误，入口初始化失败不阻塞后续流程
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
        val wsScheme = if (config.scheme == "https") "wss" else "ws"
        val query = if (queryParams.isEmpty()) "" else "?" + queryParams.entries
            .sortedBy { it.key }
            .joinToString("&") { (k, v) -> "${k.encodeURLParameter()}=${v.encodeURLParameter()}" }
        val url = "$wsScheme://${config.host}:${config.port}$path$query"
        val cookie = storedSessionCookie
        httpClient.webSocket(
            urlString = url,
            request = {
                if (cookie != null) header(HttpHeaders.Cookie, cookie)
            },
            block = block
        )
    }

    /** 共享客户端不在单个服务生命周期内关闭 */
    fun close() {}
}
