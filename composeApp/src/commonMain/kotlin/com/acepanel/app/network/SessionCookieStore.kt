package com.acepanel.app.network

/**
 * 全局 Session Cookie 存储，按面板 ID 存储登录后的 Session Cookie
 * 解决不同 ViewModel 实例间无法共享 Cookie 的问题
 */
object SessionCookieStore {
    private val store = mutableMapOf<String, String>()

    fun get(panelId: String): String? = store[panelId]

    fun set(panelId: String, cookie: String) {
        store[panelId] = cookie
    }

    fun clear(panelId: String) {
        store.remove(panelId)
    }
}
