package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val id: Int,
    val name: String?,
    val url: String,
    val username: String?,
    val password: String?,
    val advanced: AdvancedSettings = AdvancedSettings(),
) {
    companion object {
        val protocolRegex = Regex("^https?://")
    }

    val requestUrl = buildString {
        if (!url.contains("://")) {
            append("http://")
        }

        append(url)

        if (!url.endsWith("/")) {
            append("/")
        }
    }
    val visibleUrl = url.replace(protocolRegex, "")

    val displayName = name ?: visibleUrl

    val protocol = if (url.substringBefore("://", "").equals("https", ignoreCase = true)) Protocol.HTTPS else Protocol.HTTP

    @Serializable
    data class AdvancedSettings(
        val trustSelfSignedCertificates: Boolean = false,
        val basicAuth: BasicAuth = BasicAuth(false, null, null),
        val dnsOverHttps: DnsOverHttps? = null,
        val customHeaders: List<CustomHeader> = emptyList(),
    ) {
        @Serializable
        data class BasicAuth(
            val isEnabled: Boolean,
            val username: String?,
            val password: String?,
        )

        @Serializable
        data class CustomHeader(
            val key: String,
            val value: String,
        )
    }
}

enum class Protocol {
    HTTP,
    HTTPS,
}

// https://github.com/mihonapp/mihon/blob/main/core/common/src/main/kotlin/eu/kanade/tachiyomi/network/DohProviders.kt
@Serializable
enum class DnsOverHttps(val url: String, val bootstrapDnsHosts: List<String>) {
    Cloudflare(
        url = "https://cloudflare-dns.com/dns-query",
        bootstrapDnsHosts = listOf(
            "162.159.36.1",
            "162.159.46.1",
            "1.1.1.1",
            "1.0.0.1",
            "162.159.132.53",
            "2606:4700:4700::1111",
            "2606:4700:4700::1001",
            "2606:4700:4700::0064",
            "2606:4700:4700::6400",
        ),
    ),

    Google(
        url = "https://dns.google/dns-query",
        bootstrapDnsHosts = listOf(
            "8.8.4.4",
            "8.8.8.8",
            "2001:4860:4860::8888",
            "2001:4860:4860::8844",
        ),
    ),

    AdGuard(
        url = "https://dns-unfiltered.adguard.com/dns-query",
        bootstrapDnsHosts = listOf(
            "94.140.14.140",
            "94.140.14.141",
            "2a10:50c0::1:ff",
            "2a10:50c0::2:ff",
        ),
    ),

    Quad9(
        url = "https://dns.quad9.net/dns-query",
        bootstrapDnsHosts = listOf(
            "9.9.9.9",
            "149.112.112.112",
            "2620:fe::fe",
            "2620:fe::9",
        ),
    ),

    AliDNS(
        url = "https://dns.alidns.com/dns-query",
        bootstrapDnsHosts = listOf(
            "223.5.5.5",
            "223.6.6.6",
            "2400:3200::1",
            "2400:3200:baba::1",
        ),
    ),

    DNSPod(
        url = "https://doh.pub/dns-query",
        bootstrapDnsHosts = listOf(
            "1.12.12.12",
            "120.53.53.53",
        ),
    ),

    DNS360(
        url = "https://doh.360.cn/dns-query",
        bootstrapDnsHosts = listOf(
            "101.226.4.6",
            "218.30.118.6",
            "123.125.81.6",
            "140.207.198.6",
            "180.163.249.75",
            "101.199.113.208",
            "36.99.170.86",
        ),
    ),

    Quad101(
        url = "https://dns.twnic.tw/dns-query",
        bootstrapDnsHosts = listOf(
            "101.101.101.101",
            "2001:de4::101",
            "2001:de4::102",
        ),
    ),

    Mullvad(
        url = "https://dns.mullvad.net/dns-query",
        bootstrapDnsHosts = listOf(
            "194.242.2.2",
            "2a07:e340::2",
        ),
    ),

    ControlD(
        url = "https://freedns.controld.com/p0",
        bootstrapDnsHosts = listOf(
            "76.76.2.0",
            "76.76.10.0",
            "2606:1a40::",
            "2606:1a40:1::",
        ),
    ),

    Njalla(
        url = "https://dns.njal.la/dns-query",
        bootstrapDnsHosts = listOf(
            "95.215.19.53",
            "2001:67c:2354:2::53",
        ),
    ),

    Shecan(
        url = "https://free.shecan.ir/dns-query",
        bootstrapDnsHosts = listOf(
            "178.22.122.100",
            "185.51.200.2",
        ),
    ),
}
