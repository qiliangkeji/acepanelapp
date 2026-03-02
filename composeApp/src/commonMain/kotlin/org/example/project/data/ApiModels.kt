package org.example.project.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull

object StringListOrEmptySerializer : KSerializer<List<String>> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return emptyList()
        return jsonDecoder.json.decodeFromJsonElement(delegate, element)
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        delegate.serialize(encoder, value)
    }
}

object WebsiteListenListOrEmptySerializer : KSerializer<List<WebsiteListen>> {
    private val delegate = ListSerializer(WebsiteListen.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<WebsiteListen> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return emptyList()
        return jsonDecoder.json.decodeFromJsonElement(delegate, element)
    }

    override fun serialize(encoder: Encoder, value: List<WebsiteListen>) {
        delegate.serialize(encoder, value)
    }
}

// =================== 通用 ===================

@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null
)

/** 仅用于提取错误响应中的 msg 字段（后端统一用 msg 而非 message） */
@Serializable
data class ApiErrorBody(
    val msg: String = ""
)

// =================== 首页/系统 ===================

@Serializable
data class PanelInfo(
    val name: String = "",
    val locale: String = "",
    @Serializable(with = StringListOrEmptySerializer::class)
    val hidden_menu: List<String> = emptyList(),
    val custom_logo: String = ""
)

@Serializable
data class SystemInfo(
    val hostname: String = "",
    val os_name: String = "",
    val kernel_version: String = "",
    val uptime: Long = 0,
    val nets: List<SystemNetCard> = emptyList(),
    val disks: List<SystemDisk> = emptyList()
)

@Serializable
data class SystemNetCard(
    val label: String = "",
    val value: String = "",
    val name: String = ""
)

@Serializable
data class SystemDisk(
    val label: String = "",
    val value: String = "",
    val name: String = ""
)

@Serializable
data class CurrentUsageRequest(
    val nets: List<String> = emptyList(),
    val disks: List<String> = emptyList()
)

@Serializable
data class CurrentUsage(
    val cpus: List<CpuCoreUsage> = emptyList(),
    val percent: Double = 0.0,
    val percents: List<Double> = emptyList(),
    val host: HostUsage = HostUsage(),
    val mem: MemUsage = MemUsage(),
    val swap: SwapUsage = SwapUsage(),
    val disk: List<DiskPartition> = emptyList(),
    val disk_usage: List<DiskUsage> = emptyList(),
    val disk_io: List<DiskIOUsage> = emptyList(),
    val net: List<NetUsage> = emptyList(),
    val load: LoadAvg = LoadAvg(),
    val time: String = "",
    val top_processes: TopProcessesUsage = TopProcessesUsage()
)

@Serializable
data class CpuCoreUsage(
    val cpu: Int = 0,
    val modelName: String = "",
    val cores: Int = 0,
    val mhz: Double = 0.0,
    val cacheSize: Int = 0
)

@Serializable
data class HostUsage(
    val hostname: String = "",
    val uptime: Long = 0,
    val platform: String = "",
    val platformVersion: String = "",
    val kernelVersion: String = "",
    val kernelArch: String = ""
)

@Serializable
data class SwapUsage(
    val total: Long = 0,
    val used: Long = 0,
    val free: Long = 0,
    val usedPercent: Double = 0.0
)

@Serializable
data class DiskPartition(
    val device: String = "",
    val mountpoint: String = "",
    val fstype: String = ""
)

@Serializable
data class MemUsage(
    val usedPercent: Double = 0.0,
    val used: Long = 0,
    val total: Long = 0
)

@Serializable
data class DiskUsage(
    val path: String = "",
    val fstype: String = "",
    val usedPercent: Double = 0.0,
    val used: Long = 0,
    val total: Long = 0,
    val inodesTotal: Long = 0,
    val inodesUsed: Long = 0,
    val inodesFree: Long = 0,
    val inodesUsedPercent: Double = 0.0
)

@Serializable
data class DiskIOUsage(
    val name: String = "",
    val readBytes: Long = 0,
    val writeBytes: Long = 0,
    val readTime: Long = 0,
    val writeTime: Long = 0
)

@Serializable
data class NetUsage(
    val name: String = "",
    val bytesSent: Long = 0,
    val bytesRecv: Long = 0
)

@Serializable
data class LoadAvg(
    val load1: Double = 0.0,
    val load5: Double = 0.0,
    val load15: Double = 0.0
)

@Serializable
data class TopProcessesUsage(
    val cpu: List<ProcessStatItem> = emptyList(),
    val memory: List<ProcessStatItem> = emptyList(),
    val disk_io: List<ProcessStatItem> = emptyList()
)

@Serializable
data class CountInfo(
    val website: Int = 0,
    val database: Int = 0,
    val cron: Int = 0
)

// =================== 网站 ===================

@Serializable
data class WebsiteListResponse(
    val items: List<WebsiteApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class WebsiteApiItem(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val status: Boolean = false,
    val path: String = "",
    val ssl: Boolean = false,
    val remark: String = "",
    val cert_expire: String = "",
    val php: Int = 0,
    @Serializable(with = StringListOrEmptySerializer::class)
    val domains: List<String> = emptyList()
)

@Serializable
data class WebsiteStatusRequest(
    val status: Boolean
)

@Serializable
data class DeleteWebsiteRequest(
    val path: Boolean = false,
    val db: Boolean = false
)

// =================== 数据库 ===================

@Serializable
data class DatabaseListResponse(
    val items: List<DatabaseApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class DatabaseApiItem(
    val id: Long = 0,
    val name: String = "",
    val server: String = "",
    val server_id: Long = 0,
    val encoding: String = "",
    val comment: String = "",
    val type: String = ""
)

@Serializable
data class DatabaseServerListResponse(
    val items: List<DatabaseServerApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class DatabaseServerApiItem(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val status: Boolean = false,
    val host: String = "",
    val port: Int = 0,
    val remark: String = ""
)

@Serializable
data class CreateDatabaseRequest(
    val server_id: Long,
    val name: String,
    val create_user: Boolean = false,
    val username: String = "",
    val password: String = "",
    val host: String = "localhost",
    val comment: String = ""
)

// =================== 用户/鉴权 ===================

@Serializable
data class UserInfo(
    val id: Long = 0,
    val username: String = "",
    val email: String = ""
)

@Serializable
data class PublicKeyResponse(
    val key: String = ""
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val pass_code: String = "",
    val captcha_code: String = "",
    val safe_login: Boolean = false
)

@Serializable
data class CaptchaResponse(
    val required: Boolean = false,
    val image: String? = null
)

@Serializable
data class IsLoginResponse(
    val is_login: Boolean = false
)

// =================== Token 管理 ===================

@Serializable
data class UserTokenListResponse(
    val items: List<UserTokenItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class UserTokenItem(
    val id: Long = 0,
    val user_id: Long = 0,
    val token: String = "",
    val ips: List<String> = emptyList(),
    val expired_at: Long = 0
)

@Serializable
data class CreateTokenRequest(
    val user_id: Long,
    val ips: List<String> = emptyList(),
    val expired_at: Long
)

// =================== 防火墙 ===================

@Serializable
data class FirewallRuleApiItem(
    val type: String = "normal",
    val family: String = "ipv4",
    val port_start: Int = 0,
    val port_end: Int = 0,
    val protocol: String = "tcp",
    val address: String = "",
    val strategy: String = "accept",
    val direction: String = "in",
    val in_use: Boolean = false
)

@Serializable
data class FirewallRuleListResponse(
    val items: List<FirewallRuleApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateFirewallRuleRequest(
    val type: String = "normal",
    val family: String,
    val port_start: Int,
    val port_end: Int,
    val protocol: String,
    val address: String = "",
    val strategy: String,
    val direction: String
)

// =================== 计划任务 ===================

@Serializable
data class CronTaskApiItem(
    val id: Long = 0,
    val name: String = "",
    val status: Boolean = false,
    val type: String = "shell",
    val time: String = "",
    val log: String = ""
)

@Serializable
data class CronTaskListResponse(
    val items: List<CronTaskApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CronStatusRequest(val status: Boolean)

// =================== 日志 ===================

@Serializable
data class LogApiEntry(
    val time: String = "",
    val level: String = "",
    val msg: String = "",
    val type: String = "",
    val operator_name: String = ""
)

@Serializable
data class SshLogApiItem(
    val time: String = "",
    val user: String = "",
    val ip: String = "",
    val port: String = "",
    val method: String = "",
    val status: String = ""
)

// =================== 监控 ===================

@Serializable
data class ProcessStatItem(
    val pid: Int = 0,
    val name: String = "",
    val username: String = "",
    val command: String = "",
    val value: Double = 0.0,
    val read: Double = 0.0,
    val write: Double = 0.0
)

// =================== 面板设置 ===================

@Serializable
data class PanelSettingData(
    val name: String = "",
    val channel: String = "stable",
    val locale: String = "zh",
    val entrance: String = "",
    val entrance_error: String = "nginx",
    val login_captcha: Boolean = false,
    val offline_mode: Boolean = false,
    val auto_update: Boolean = true,
    val lifetime: Int = 60,
    val website_path: String = "",
    val backup_path: String = "",
    val project_path: String = "",
    val port: Int = 8888
)

@Serializable
data class UpdateSettingResponse(val restart: Boolean = false)

// =================== 软件商店 ===================

@Serializable
data class AppCategoryItem(
    val label: String = "",
    val value: String = ""
)

@Serializable
data class AppChannelInfo(
    val slug: String = "",
    val name: String = "",
    val version: String = ""
)

@Serializable
data class AppDetailItem(
    val slug: String = "",
    val name: String = "",
    val description: String = "",
    val categories: List<String> = emptyList(),
    val channels: List<AppChannelInfo> = emptyList(),
    val installed: Boolean = false,
    val installed_channel: String = "",
    val installed_version: String = "",
    val update_exist: Boolean = false,
    val show: Boolean = true
)

@Serializable
data class AppListResponse(
    val items: List<AppDetailItem> = emptyList(),
    val total: Int = 0
)

// =================== 网站创建 ===================

@Serializable
data class InstalledEnvironment(
    val php: List<LabeledIntValue> = emptyList(),
    val db: List<LabeledStringValue> = emptyList(),
    val webserver: String = ""
)

@Serializable
data class LabeledIntValue(
    val label: String = "",
    val value: Int = 0
)

@Serializable
data class LabeledStringValue(
    val label: String = "",
    val value: String = ""
)

@Serializable
data class CreateWebsiteRequest(
    val type: String,
    val name: String,
    val listens: List<String>,
    val domains: List<String>,
    val path: String = "",
    val php: Int = 0,
    val proxy: String = "",
    val db: Boolean = false,
    val db_type: String = "",
    val db_name: String = "",
    val db_user: String = "",
    val db_password: String = "",
    val remark: String = ""
)

// =================== 网站详情 ===================

@Serializable
data class WebsiteListen(
    val address: String = "",
    val args: List<String> = emptyList()
)

@Serializable
data class WebsiteDetailData(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    @Serializable(with = WebsiteListenListOrEmptySerializer::class)
    val listens: List<WebsiteListen> = emptyList(),
    @Serializable(with = StringListOrEmptySerializer::class)
    val domains: List<String> = emptyList(),
    val path: String = "",
    val root: String = "",
    val ssl: Boolean = false,
    val ssl_not_before: String = "",
    val ssl_not_after: String = "",
    @Serializable(with = StringListOrEmptySerializer::class)
    val ssl_dns_names: List<String> = emptyList(),
    val ssl_issuer: String = "",
    val php: Int = 0,
    val access_log: String = "",
    val error_log: String = "",
    val stat_enabled: Boolean = false
)

// =================== 任务中心 ===================

@Serializable
data class TaskItem(
    val id: Long = 0,
    val name: String = "",
    val status: String = "waiting",
    val log: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class TaskListResponse(
    val items: List<TaskItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class TaskStatusResponse(
    val task: Boolean = false
)

// =================== 项目管理 ===================

@Serializable
data class ProjectListItem(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val description: String = "",
    val root_dir: String = "",
    val exec_start: String = "",
    val user: String = "",
    val restart: String = "",
    val status: String = "",
    val enabled: Boolean = false,
    val pid: Int = 0,
    val memory: Long = 0,
    val cpu: Double = 0.0,
    val uptime: String = ""
)

@Serializable
data class ProjectListResponse(
    val items: List<ProjectListItem> = emptyList(),
    val total: Int = 0
)

// =================== Systemctl ===================

@Serializable
data class SystemctlStatusResponse(
    val status: String = ""
)

@Serializable
data class SystemctlIsEnabledResponse(
    val enabled: Boolean = false
)

// =================== SSL 证书 ===================

@Serializable
data class CertListItem(
    val id: Long = 0,
    val account_id: Long = 0,
    val website_id: Long = 0,
    val dns_id: Long = 0,
    val type: String = "",
    val domains: List<String> = emptyList(),
    val auto_renewal: Boolean = false,
    val next_renewal: String = "",
    val cert: String = "",
    val key: String = "",
    val cert_url: String = "",
    val not_before: String = "",
    val not_after: String = "",
    val issuer: String = "",
    val dns_names: List<String> = emptyList(),
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class CertListResponse(
    val items: List<CertListItem> = emptyList(),
    val total: Int = 0
)

// =================== 文件管理 ===================

@Serializable
data class FileListItem(
    val name: String = "",
    val full: String = "",
    val size: String = "",
    val mode: String = "",
    val owner: String = "",
    val group: String = "",
    val dir: Boolean = false,
    val hidden: Boolean = false,
    val symlink: Boolean = false,
    val modify: String = ""
)

@Serializable
data class FileListResponse(
    val items: List<FileListItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class FileContentResponse(
    val mime: String = "",
    val content: String = ""
)

@Serializable
data class FileSaveRequest(
    val path: String,
    val content: String
)

// =================== 备份管理 ===================

@Serializable
data class BackupFileItem(
    val name: String = "",
    val path: String = "",
    val size: String = "",
    val time: String = ""
)

@Serializable
data class BackupListResponse(
    val items: List<BackupFileItem> = emptyList(),
    val total: Int = 0
)

// =================== 数据库服务器创建/编辑 ===================

@Serializable
data class CreateDatabaseServerRequest(
    val name: String,
    val type: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val remark: String = ""
)

@Serializable
data class UpdateDatabaseServerRequest(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val remark: String = ""
)

// =================== 数据库用户 ===================

@Serializable
data class DatabaseUserApiItem(
    val id: Long = 0,
    val server_id: Long = 0,
    val username: String = "",
    val password: String = "",
    val host: String = "",
    val status: String = "",
    val privileges: List<String> = emptyList(),
    val remark: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class DatabaseUserListResponse(
    val items: List<DatabaseUserApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateDatabaseUserRequest(
    val server_id: Long,
    val username: String,
    val password: String,
    val host: String = "localhost",
    val privileges: List<String> = emptyList(),
    val remark: String = ""
)

// =================== WebHook ===================

@Serializable
data class WebHookItem(
    val id: Long = 0,
    val name: String = "",
    val key: String = "",
    val script: String = "",
    val raw: Boolean = false,
    val user: String = "",
    val status: Boolean = true,
    val call_count: Long = 0,
    val last_call_at: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class WebHookListResponse(
    val items: List<WebHookItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateWebHookRequest(
    val name: String,
    val script: String,
    val raw: Boolean = false,
    val user: String = "root"
)

@Serializable
data class UpdateWebHookRequest(
    val name: String,
    val script: String,
    val raw: Boolean = false,
    val user: String = "root",
    val status: Boolean = true
)

// =================== SSH 主机 ===================

@Serializable
data class SshClientConfigData(
    val auth_method: String = "password",
    val host: String = "",
    val user: String = "",
    val password: String = "",
    val key: String = "",
    val passphrase: String = "",
    val timeout: Int = 10
)

@Serializable
data class SshHostItem(
    val id: Long = 0,
    val name: String = "",
    val host: String = "",
    val port: Int = 22,
    val config: SshClientConfigData = SshClientConfigData(),
    val remark: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class SshHostListResponse(
    val items: List<SshHostItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateSshRequest(
    val name: String,
    val host: String,
    val port: Int = 22,
    val auth_method: String = "password",
    val user: String,
    val password: String = "",
    val key: String = "",
    val passphrase: String = "",
    val remark: String = ""
)

// =================== 备份存储 ===================

@Serializable
data class BackupStorageInfo(
    val access_key: String = "",
    val secret_key: String = "",
    val style: String = "",
    val region: String = "",
    val endpoint: String = "",
    val scheme: String = "",
    val bucket: String = "",
    val username: String = "",
    val password: String = "",
    val private_key: String = "",
    val url: String = "",
    val path: String = "",
    val port: Int = 0,
    val host: String = ""
)

@Serializable
data class BackupStorageItem(
    val id: Long = 0,
    val type: String = "",
    val name: String = "",
    val info: BackupStorageInfo = BackupStorageInfo(),
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class BackupStorageListResponse(
    val items: List<BackupStorageItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateBackupStorageRequest(
    val type: String,
    val name: String,
    val info: BackupStorageInfo
)

// =================== 用户管理 ===================

@Serializable
data class UserItem(
    val id: Long = 0,
    val username: String = "",
    val email: String = "",
    val created_at: String = ""
)

@Serializable
data class UserListResponse(
    val items: List<UserItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val email: String
)

// =================== Redis 键管理 ===================

@Serializable
data class RedisKVItem(
    val key: String = "",
    val value: String = "",
    val type: String = "",
    val size: Long = 0,
    val length: Long = 0,
    val ttl: Long = -1
)

@Serializable
data class RedisDataResponse(
    val items: List<RedisKVItem> = emptyList(),
    val total: Int = 0
)

// =================== ACME 账号 ===================

@Serializable
data class AcmeAccountItem(
    val id: Long = 0,
    val email: String = "",
    val ca: String = "",
    val key_type: String = "",
    val kid: String = "",
    val hmac_encoded: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class AcmeAccountListResponse(
    val items: List<AcmeAccountItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateAcmeAccountRequest(
    val ca: String,
    val email: String,
    val key_type: String,
    val kid: String = "",
    val hmac_encoded: String = ""
)

// =================== DNS 提供商 ===================

@Serializable
data class DnsParamData(
    val ak: String = "",
    val sk: String = ""
)

@Serializable
data class DnsProviderItem(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val dns_param: DnsParamData = DnsParamData(),
    val created_at: String = "",
    val updated_at: String = ""
)

@Serializable
data class DnsProviderListResponse(
    val items: List<DnsProviderItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateDnsProviderRequest(
    val type: String,
    val name: String,
    val data: DnsParamData
)

// =================== 用户 2FA ===================

@Serializable
data class TwoFaInfo(
    val img: String = "",     // Base64 PNG 二维码
    val url: String = "",     // otpauth:// URI
    val secret: String = ""   // TOTP 密钥
)

// =================== 网站统计 ===================

@Serializable
data class StatTotals(
    val pv: Long = 0,
    val uv: Long = 0,
    val ip: Long = 0,
    val bandwidth: Long = 0,
    val requests: Long = 0,
    val errors: Long = 0,
    val spiders: Long = 0,
    val status_2xx: Long = 0,
    val status_3xx: Long = 0,
    val status_4xx: Long = 0,
    val status_5xx: Long = 0
)

@Serializable
data class StatSeries(
    val key: String = "",
    val pv: Long = 0,
    val uv: Long = 0,
    val ip: Long = 0,
    val bandwidth: Long = 0,
    val requests: Long = 0,
    val errors: Long = 0
)

@Serializable
data class StatSite(
    val id: Long = 0,
    val name: String = ""
)

@Serializable
data class WebsiteStatOverview(
    val current: StatTotals = StatTotals(),
    val previous: StatTotals = StatTotals(),
    val series: List<StatSeries> = emptyList(),
    val previous_series: List<StatSeries> = emptyList(),
    val sites: List<StatSite> = emptyList()
)

@Serializable
data class WebsiteStatSetting(
    val days: Int = 30,
    val err_buf_max: Int = 0,
    val uv_max_keys: Int = 0,
    val ip_max_keys: Int = 0,
    val detail_max_keys: Int = 0,
    val body_enabled: Boolean = false
)

@Serializable
data class WebsiteStatRealtime(
    val bandwidth: Double = 0.0,
    val bandwidth_in: Double = 0.0,
    val rps: Double = 0.0
)

@Serializable
data class WebsiteStatSiteItem(
    val site: String = "",
    val pv: Long = 0,
    val uv: Long = 0,
    val ip: Long = 0,
    val bandwidth: Long = 0,
    val bandwidth_in: Long = 0,
    val requests: Long = 0,
    val errors: Long = 0,
    val spiders: Long = 0,
    val request_time_sum: Long = 0,
    val request_time_count: Long = 0,
    val status_2xx: Long = 0,
    val status_3xx: Long = 0,
    val status_4xx: Long = 0,
    val status_5xx: Long = 0
)

@Serializable
data class WebsiteStatSpiderItem(
    val spider: String = "",
    val requests: Long = 0,
    val percent: Double = 0.0
)

@Serializable
data class WebsiteStatClientItem(
    val browser: String = "",
    val os: String = "",
    val requests: Long = 0
)

@Serializable
data class WebsiteStatNameCountItem(
    val name: String = "",
    val requests: Long = 0
)

@Serializable
data class WebsiteStatIpItem(
    val ip: String = "",
    val country: String = "",
    val region: String = "",
    val city: String = "",
    val isp: String = "",
    val requests: Long = 0,
    val bandwidth: Long = 0
)

@Serializable
data class WebsiteStatGeoItem(
    val country: String = "",
    val region: String = "",
    val city: String = "",
    val requests: Long = 0,
    val bandwidth: Long = 0
)

@Serializable
data class WebsiteStatUriItem(
    val uri: String = "",
    val requests: Long = 0,
    val bandwidth: Long = 0,
    val errors: Long = 0,
    val request_time_sum: Long = 0,
    val request_time_count: Long = 0
)

@Serializable
data class WebsiteStatErrorItem(
    val id: Long = 0,
    val site: String = "",
    val uri: String = "",
    val method: String = "",
    val status: Int = 0,
    val ip: String = "",
    val ua: String = "",
    val body: String = "",
    val created_at: String = ""
)

@Serializable
data class WebsiteStatSiteResponse(
    val items: List<WebsiteStatSiteItem> = emptyList()
)

@Serializable
data class WebsiteStatSpiderResponse(
    val items: List<WebsiteStatSpiderItem> = emptyList(),
    val total: Long = 0
)

@Serializable
data class WebsiteStatClientResponse(
    val items: List<WebsiteStatClientItem> = emptyList(),
    val browsers: List<WebsiteStatNameCountItem> = emptyList(),
    val os: List<WebsiteStatNameCountItem> = emptyList()
)

@Serializable
data class WebsiteStatIpResponse(
    val items: List<WebsiteStatIpItem> = emptyList(),
    val total: Long = 0
)

@Serializable
data class WebsiteStatGeoResponse(
    val items: List<WebsiteStatGeoItem> = emptyList()
)

@Serializable
data class WebsiteStatUriResponse(
    val items: List<WebsiteStatUriItem> = emptyList(),
    val total: Long = 0
)

@Serializable
data class WebsiteStatErrorResponse(
    val items: List<WebsiteStatErrorItem> = emptyList(),
    val total: Long = 0
)

// =================== 防火墙扫描审计 ===================

@Serializable
data class FirewallScanSetting(
    val enabled: Boolean = false,
    val days: Int = 30,
    val interfaces: List<String> = emptyList(),
    val auto_block: Boolean = false,
    val block_threshold: Int = 100,
    val block_window: Int = 60,
    val block_duration: Int = 24,
    val whitelist: List<String> = emptyList()
)

@Serializable
data class FirewallScanSummary(
    val total_count: Long = 0,
    val unique_ips: Long = 0,
    val unique_ports: Long = 0
)

@Serializable
data class ScanEvent(
    val id: Long = 0,
    val source_ip: String = "",
    val port: Int = 0,
    val protocol: String = "",
    val date: String = "",
    val count: Long = 0,
    val country: String = "",
    val region: String = "",
    val city: String = "",
    val isp: String = "",
    val first_seen: String = "",
    val last_seen: String = ""
)

@Serializable
data class ScanEventListResponse(
    val items: List<ScanEvent> = emptyList(),
    val total: Long = 0
)

@Serializable
data class ScanTopItem(
    val key: String = "",
    val count: Long = 0
)

// =================== 进程管理 ===================

@Serializable
data class ProcessApiItem(
    val pid: Int = 0,
    val name: String = "",
    val status: String = "",
    val username: String = "",
    val cpu: Double = 0.0,
    val rss: Long = 0,
    val ppid: Int = 0,
    val num_threads: Int = 0,
    val start_time: String = "",
    val command: String = ""
)

@Serializable
data class ProcessListResponse(
    val items: List<ProcessApiItem> = emptyList(),
    val total: Long = 0
)

// =================== 防火墙 IP 规则 ===================

@Serializable
data class IpRuleApiItem(
    val family: String = "ipv4",
    val protocol: String = "tcp",
    val address: String = "",
    val strategy: String = "accept",
    val direction: String = "in"
)

@Serializable
data class IpRuleListResponse(
    val items: List<IpRuleApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateIpRuleRequest(
    val family: String,
    val protocol: String,
    val address: String,
    val strategy: String,
    val direction: String
)

// =================== 防火墙端口转发 ===================

@Serializable
data class ForwardRuleApiItem(
    val protocol: String = "tcp",
    val port: Int = 0,
    val target_ip: String = "",
    val target_port: Int = 0
)

@Serializable
data class ForwardRuleListResponse(
    val items: List<ForwardRuleApiItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateForwardRuleRequest(
    val protocol: String,
    val port: Int,
    val target_ip: String,
    val target_port: Int
)

// =================== 计划任务创建 ===================

@Serializable
data class CreateCronRequest(
    val name: String,
    val type: String,
    val time: String,
    val keep: Int = 7,
    val script: String = "",
    val url: String = "",
    val method: String = "GET",
    val sub_type: String = "",
    val targets: List<String> = emptyList()
)

// =================== 系统监控历史 ===================

@Serializable
data class MonitorHistoryResponse(
    val cpu: List<Double> = emptyList(),
    val mem: List<Double> = emptyList(),
    val swap: List<Double> = emptyList(),
    val times: List<Long> = emptyList()
)

@Serializable
data class MonitorSetting(
    val enabled: Boolean = true,
    val days: Int = 30,
    val interval: Int = 5
)

// =================== SSL 证书创建 ===================

@Serializable
data class CreateCertRequest(
    val type: String,
    val domains: List<String>,
    val auto_renewal: Boolean = false,
    val account_id: Long = 0,
    val dns_id: Long = 0,
    val website_id: Long = 0
)

@Serializable
data class UploadCertRequest(
    val cert: String,
    val key: String
)

// =================== 备份创建 ===================

@Serializable
data class CreateBackupRequest(
    val target: String = "",
    val storage: Long = 0
)

// =================== 计划任务更新 ===================

@Serializable
data class UpdateCronRequest(
    val name: String = "",
    val time: String = "",
    val keep: Int = 1,
    val script: String = "",
    val url: String = "",
    val sub_type: String = "",
    val targets: List<String> = emptyList()
)

// =================== Redis 键创建 ===================

@Serializable
data class CreateRedisKeyRequest(
    val server_id: Long,
    val db: Int = 0,
    val key: String,
    val value: String,
    val type: String,
    val ttl: Long = -1
)

// =================== 数据库用户更新 ===================

@Serializable
data class UpdateDatabaseUserRequest(
    val password: String = "",
    val privileges: List<String> = emptyList()
)

// =================== 项目创建 ===================

@Serializable
data class CreateProjectRequest(
    val name: String,
    val type: String,
    val description: String = "",
    val root_dir: String = "",
    val working_dir: String = "",
    val exec_start: String = "",
    val user: String = "root",
    val restart: String = "on-failure"
)

// =================== 运行时面板状态（客户端本地聚合） ===================

data class PanelRuntimeStatus(
    val isOnline: Boolean = false,
    val cpuPercent: Double = 0.0,
    val memPercent: Double = 0.0,
    val diskPercent: Double = 0.0,
    val uptime: Long = 0,
    val hostname: String = "",
    val osName: String = ""
)
