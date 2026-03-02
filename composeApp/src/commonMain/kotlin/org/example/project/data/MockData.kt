package org.example.project.data

object MockData {
    val panels = listOf(
        Panel(
            id = "1",
            name = "生产服务器",
            ip = "192.168.1.100",
            os = "CentOS 8.2 · 4核 8G",
            status = PanelStatus.RUNNING,
            cpu = 45,
            memory = 62,
            disk = 38,
            uptime = "已运行 32天"
        ),
        Panel(
            id = "2",
            name = "测试环境",
            ip = "10.0.0.50",
            os = "Ubuntu 22.04 · 2核 4G",
            status = PanelStatus.RUNNING,
            cpu = 28,
            memory = 45,
            disk = 52
        ),
        Panel(
            id = "3",
            name = "开发服务器",
            ip = "172.16.0.10",
            os = "Debian 11 · 2核 4G",
            status = PanelStatus.STOPPED,
            cpu = 0,
            memory = 0,
            disk = 0
        )
    )

    val websites = listOf(
        Website(
            id = "1",
            domain = "example.com",
            url = "https://www.example.com",
            status = WebsiteStatus.RUNNING,
            framework = "PHP 8.1",
            version = "Nginx"
        ),
        Website(
            id = "2",
            domain = "blog.example.com",
            url = "https://www.example.com/blog",
            status = WebsiteStatus.RUNNING,
            framework = "Node.js 18",
            version = "Nginx"
        ),
        Website(
            id = "3",
            domain = "api.example.com",
            url = "https://www.example.com/api",
            status = WebsiteStatus.STOPPED,
            framework = "Python 3.10",
            version = "Gunicorn"
        )
    )

    val databases = listOf(
        Database(
            id = "1",
            name = "wordpress_db",
            tables = 12,
            size = "45.2MB",
            charset = "utf8mb4"
        ),
        Database(
            id = "2",
            name = "app_database",
            tables = 24,
            size = "78.5MB",
            charset = "utf8mb4"
        ),
        Database(
            id = "3",
            name = "test_db",
            tables = 8,
            size = "32.3MB",
            charset = "utf8"
        )
    )

    val serverStats = ServerStats(
        totalServers = 3,
        runningServers = 2,
        stoppedServers = 1,
        avgCpu = 48,
        avgMemory = 55,
        avgDisk = 42,
        totalWebsites = 12,
        runningWebsites = 10,
        stoppedWebsites = 2
    )

}
