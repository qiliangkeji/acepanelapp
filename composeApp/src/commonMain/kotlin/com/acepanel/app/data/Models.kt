package com.acepanel.app.data

data class Panel(
    val id: String,
    val name: String,
    val ip: String,
    val os: String,
    val status: PanelStatus,
    val cpu: Int = 0,
    val memory: Int = 0,
    val disk: Int = 0,
    val uptime: String = ""
)

enum class PanelStatus {
    RUNNING,
    STOPPED
}

data class Website(
    val id: String,
    val domain: String,
    val url: String,
    val status: WebsiteStatus,
    val framework: String,
    val version: String
)

enum class WebsiteStatus {
    RUNNING,
    STOPPED
}

data class Database(
    val id: String,
    val name: String,
    val tables: Int,
    val size: String,
    val charset: String
)

data class ServerStats(
    val totalServers: Int,
    val runningServers: Int,
    val stoppedServers: Int,
    val avgCpu: Int,
    val avgMemory: Int,
    val avgDisk: Int,
    val totalWebsites: Int,
    val runningWebsites: Int,
    val stoppedWebsites: Int
)
