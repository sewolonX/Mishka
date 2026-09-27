package top.yukonga.mishka.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * mihomo 配置覆写模型。字段按 mihomo `RawConfig` json tag 命名，
 * 通过 `--override-json <path>` 参数传给 mihomo，在 yaml.Unmarshal 之后、
 * ParseRawConfig 之前经 `json.NewDecoder(...).Decode(rawCfg)` 注入。
 *
 * 序列化时 null 字段不应输出，mihomo decode 跳过未提及字段即保留 RawConfig 原值。
 *
 * 全树只读：[OverrideJsonStore] 把同一个实例作为 StateFlow 的值发布出去，就地改字段会让
 * StateFlow 的相等去重把这次更新静默吞掉。修改一律经 `copy()`。
 */
@Serializable
data class ConfigurationOverride(
    @SerialName("port") val httpPort: Int? = null,
    @SerialName("socks-port") val socksPort: Int? = null,
    @SerialName("redir-port") val redirPort: Int? = null,
    @SerialName("tproxy-port") val tproxyPort: Int? = null,
    @SerialName("mixed-port") val mixedPort: Int? = null,
    @SerialName("routing-mark") val routingMark: Int? = null,
    @SerialName("allow-lan") val allowLan: Boolean? = null,
    @SerialName("ipv6") val ipv6: Boolean? = null,
    @SerialName("bind-address") val bindAddress: String? = null,
    @SerialName("log-level") val logLevel: String? = null,
    @SerialName("mode") val mode: String? = null,
    @SerialName("external-controller") val externalController: String? = null,
    @SerialName("secret") val secret: String? = null,
    @SerialName("unified-delay") val unifiedDelay: Boolean? = null,
    @SerialName("geodata-mode") val geodataMode: Boolean? = null,
    @SerialName("tcp-concurrent") val tcpConcurrent: Boolean? = null,
    @SerialName("find-process-mode") val findProcessMode: String? = null,
    @SerialName("dns") val dns: DnsOverride? = null,
    @SerialName("sniffer") val sniffer: SnifferOverride? = null,
    @SerialName("tun") val tun: TunOverride? = null,
    @SerialName("profile") val profile: ProfileOverride? = null,
    @SerialName("listeners") val listeners: List<Map<String, JsonElement>>? = null,
)

@Serializable
data class DnsOverride(
    @SerialName("enable") val enable: Boolean? = null,
    @SerialName("listen") val listen: String? = null,
    @SerialName("ipv6") val ipv6: Boolean? = null,
    @SerialName("prefer-h3") val preferH3: Boolean? = null,
    @SerialName("use-hosts") val useHosts: Boolean? = null,
    @SerialName("enhanced-mode") val enhancedMode: String? = null,
    @SerialName("nameserver") val nameserver: List<String>? = null,
    @SerialName("fallback") val fallback: List<String>? = null,
    @SerialName("default-nameserver") val defaultNameserver: List<String>? = null,
    @SerialName("fake-ip-filter") val fakeIpFilter: List<String>? = null,
)

@Serializable
data class SnifferOverride(
    @SerialName("enable") val enable: Boolean? = null,
    @SerialName("force-dns-mapping") val forceDnsMapping: Boolean? = null,
    @SerialName("parse-pure-ip") val parsePureIp: Boolean? = null,
    @SerialName("override-destination") val overrideDestination: Boolean? = null,
    @SerialName("force-domain") val forceDomain: List<String>? = null,
    @SerialName("skip-domain") val skipDomain: List<String>? = null,
)

/**
 * RawTun 覆写。不含 `inet4-address`（mihomo config.go:278 字段被注释），
 * VPN 模式 TUN v4 地址由 VpnService 分配，ROOT 模式 mihomo 用默认值。
 */
@Serializable
data class TunOverride(
    @SerialName("enable") val enable: Boolean? = null,
    @SerialName("device") val device: String? = null,
    @SerialName("stack") val stack: String? = null,
    @SerialName("file-descriptor") val fileDescriptor: Int? = null,
    @SerialName("auto-route") val autoRoute: Boolean? = null,
    @SerialName("auto-detect-interface") val autoDetectInterface: Boolean? = null,
    @SerialName("route-exclude-address") val routeExcludeAddress: List<String>? = null,
    @SerialName("inet6-address") val inet6Address: List<String>? = null,
    @SerialName("dns-hijack") val dnsHijack: List<String>? = null,
    @SerialName("include-package") val includePackage: List<String>? = null,
    @SerialName("exclude-package") val excludePackage: List<String>? = null,
    @SerialName("iproute2-table-index") val iproute2TableIndex: Int? = null,
    @SerialName("iproute2-rule-index") val iproute2RuleIndex: Int? = null,
    @SerialName("mtu") val mtu: Int? = null,
    @SerialName("gso") val gso: Boolean? = null,
    @SerialName("gso-max-size") val gsoMaxSize: Int? = null,
)

@Serializable
data class ProfileOverride(
    @SerialName("store-selected") val storeSelected: Boolean? = null,
    @SerialName("store-fake-ip") val storeFakeIp: Boolean? = null,
)

/**
 * 解析用户的 external-controller 设置：trim、空字符串视为未设置、默认 `127.0.0.1:9090`，
 * 并把监听地址 `0.0.0.0` 替换为客户端可连的 `127.0.0.1`。
 */
fun ConfigurationOverride.resolveExternalController(): String =
    (externalController?.trim()?.takeIf { it.isNotEmpty() } ?: "127.0.0.1:9090")
        .replace("0.0.0.0", "127.0.0.1")

/**
 * 解析用户的 secret：trim、空字符串视为未设置（让调用方 fallback 到 random 生成值）。
 */
fun ConfigurationOverride.resolveSecretOrNull(): String? =
    secret?.trim()?.takeIf { it.isNotEmpty() }

// === eBPF listener helpers ===

/** 从 override.listeners 中提取 type=ebpf 的第一个条目，转为 [EbpfOverride]。 */
fun ConfigurationOverride.extractEbpf(): EbpfOverride? {
    val listeners = listeners ?: return null
    val map = listeners.firstOrNull {
        (it["type"] as? JsonPrimitive)?.content == "ebpf"
    } ?: return null
    return map.toEbpfOverride()
}

/** 把 [EbpfOverride] 写入 override.listeners：保留非 ebpf 条目，替换/追加 ebpf 条目。 */
fun ConfigurationOverride.withEbpf(ebpf: EbpfOverride?, sharedIfaces: List<String> = emptyList()): ConfigurationOverride {
    val existing = listeners?.filter {
        (it["type"] as? JsonPrimitive)?.content != "ebpf"
    } ?: emptyList()
    val newList = if (ebpf != null && ebpf.enabled) {
        existing + ebpf.toJsonMap(sharedIfaces)
    } else {
        existing.ifEmpty { null }
    }
    return copy(listeners = newList)
}

/** 用户级 eBPF 配置，序列化为 mihomo `listeners` 数组中的一个 ebpf 条目。 */
@Serializable
data class EbpfOverride(
    val enabled: Boolean = false,
    val mode: String = "local",
    val network: List<String> = listOf("tcp", "udp"),
    val udpTimeout: Long = 300,
    // local 本机路径
    val localDataPlane: String = "cgroup",
    val localDnsMode: String = "hijack",
    val localIpv6: Boolean = true,
    val localBypassPrivateAddress: Boolean = true,
    val localIncludePackage: List<String> = emptyList(),
    val localExcludePackage: List<String> = emptyList(),
    // shared 共享路径
    val sharedDataPlane: String = "packet_rewrite",
    val sharedDnsMode: String = "hijack",
    val sharedIpv6: Boolean = true,
    val sharedBypassPrivateAddress: Boolean = true,
)

private fun EbpfOverride.toJsonMap(sharedIfaces: List<String> = emptyList()): Map<String, JsonElement> = buildMap {
    put("name", JsonPrimitive("ebpf-inbound"))
    put("type", JsonPrimitive("ebpf"))
    put("mode", JsonPrimitive(mode))
    put("network", JsonArray(network.map { JsonPrimitive(it) }))
    put("udp-timeout", JsonPrimitive(udpTimeout))
    val local = buildJsonObject {
        put("data-plane", JsonPrimitive(localDataPlane))
        put("dns-mode", JsonPrimitive(localDnsMode))
        put("ipv6", JsonPrimitive(localIpv6))
        put("bypass-private-address", JsonPrimitive(localBypassPrivateAddress))
        if (localIncludePackage.isNotEmpty()) {
            put("include-package", JsonArray(localIncludePackage.map { JsonPrimitive(it) }))
        }
        if (localExcludePackage.isNotEmpty()) {
            put("exclude-package", JsonArray(localExcludePackage.map { JsonPrimitive(it) }))
        }
    }
    put("local", local)
    // shared/hybrid 模式：复用 ROOT 热点接口配置
    if (mode == "shared" || mode == "hybrid") {
        val shared = buildJsonObject {
            put("data-plane", JsonPrimitive(sharedDataPlane))
            if (sharedIfaces.isNotEmpty()) {
                put("interface", JsonArray(sharedIfaces.map { JsonPrimitive(it) }))
            }
            put("dns-mode", JsonPrimitive(sharedDnsMode))
            put("ipv6", JsonPrimitive(sharedIpv6))
            put("bypass-private-address", JsonPrimitive(sharedBypassPrivateAddress))
        }
        put("shared", shared)
    }
}

private fun Map<String, JsonElement>.toEbpfOverride(): EbpfOverride {
    val local = (get("local") as? JsonObject)?.jsonObject
    val shared = (get("shared") as? JsonObject)?.jsonObject
    return EbpfOverride(
        enabled = true,
        mode = (get("mode") as? JsonPrimitive)?.content ?: "local",
        network = (get("network") as? JsonArray)?.map {
            it.jsonPrimitive.content
        } ?: listOf("tcp", "udp"),
        udpTimeout = (get("udp-timeout") as? JsonPrimitive)?.content?.toLongOrNull() ?: 300,
        localDataPlane = (local?.get("data-plane") as? JsonPrimitive)?.content ?: "cgroup",
        localDnsMode = (local?.get("dns-mode") as? JsonPrimitive)?.content ?: "hijack",
        localIpv6 = (local?.get("ipv6") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
        localBypassPrivateAddress = (local?.get("bypass-private-address") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
        localIncludePackage = (local?.get("include-package") as? JsonArray)?.map {
            it.jsonPrimitive.content
        } ?: emptyList(),
        localExcludePackage = (local?.get("exclude-package") as? JsonArray)?.map {
            it.jsonPrimitive.content
        } ?: emptyList(),
        sharedDataPlane = (shared?.get("data-plane") as? JsonPrimitive)?.content ?: "packet_rewrite",
        sharedDnsMode = (shared?.get("dns-mode") as? JsonPrimitive)?.content ?: "hijack",
        sharedIpv6 = (shared?.get("ipv6") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
        sharedBypassPrivateAddress = (shared?.get("bypass-private-address") as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true,
    )
}
