package com.acepanel.app.data

import kotlinx.serialization.Serializable

/**
 * 面板连接配置，本地持久化存储
 */
@Serializable
data class PanelConfig(
    val id: String,
    val name: String,
    val scheme: String = "http",        // http 或 https
    val host: String,
    val port: Int = 8888,
    val entrance: String = "",          // 安全入口路径，空表示根路径 /
    val authMode: String = "token",     // "token" 或 "session"
    // Token 鉴权
    val tokenId: Long = 0,
    val tokenSecret: String = "",
    // Session 鉴权（用于自动重登和 WebSocket）
    val sessionUsername: String = "",
    val sessionPassword: String = "",
    // 自定义 User-Agent，用于面板开启 UA 绑定时匹配允许列表
    val userAgent: String = "",
    // 备注
    val remark: String = ""
) {
    /** 构建面板基础 URL */
    fun baseUrl(): String = "$scheme://$host:$port"

    /** 规范化入口路径：去掉首尾斜杠 */
    private fun normalizedEntrance(): String = entrance.trim().trim('/')

    /** Token 模式请求 URL（带入口前缀） */
    fun apiUrl(path: String): String {
        val entrancePath = normalizedEntrance()
        return if (entrancePath.isNotEmpty() && authMode == "token") {
            "${baseUrl()}/$entrancePath$path"
        } else {
            "${baseUrl()}$path"
        }
    }

    /** Session 模式初始化入口 URL */
    fun entranceUrl(): String {
        val entrancePath = normalizedEntrance()
        return if (entrancePath.isEmpty()) baseUrl() else "${baseUrl()}/$entrancePath"
    }

    /** 检查 Token 配置是否完整 */
    fun isTokenConfigValid(): Boolean = authMode == "token" && tokenId > 0 && tokenSecret.isNotEmpty()
}
