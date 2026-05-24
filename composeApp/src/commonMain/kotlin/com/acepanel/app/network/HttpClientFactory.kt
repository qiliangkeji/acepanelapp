package com.acepanel.app.network

import io.ktor.client.*

/**
 * 平台特定的 HttpClient 工厂
 * Android: OkHttp + 信任所有证书（支持自签名）+ 超时配置
 * iOS: Darwin 默认配置
 */
expect fun createHttpClient(): HttpClient
