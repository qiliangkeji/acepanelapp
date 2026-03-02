package org.example.project.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * iOS 实现：Darwin 引擎
 */
actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        // 禁止自动跟随重定向，原因同 Android 端：保留入口验证的 302 Set-Cookie
        followRedirects = false
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 8_000L
            requestTimeoutMillis = 20_000L
            socketTimeoutMillis = 20_000L
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                explicitNulls = false
                encodeDefaults = false
            })
        }
        install(WebSockets)
        expectSuccess = false
    }
}
