package moe.nemesiss.hostman.model

import io.netty.resolver.HostsFileEntries
import moe.nemesiss.hostman.ui.compose.HostEntry

data class HostEntries(val ipv4: Map<String, HostEntry> = emptyMap(), val ipv6: Map<String, HostEntry> = emptyMap()) {

    companion object {

        const val HOST_FILE_PATH = "/etc/hosts"

        fun fromNettyHostFileEntries(nettyEntries: HostsFileEntries): HostEntries {
            val ipv4 =
                nettyEntries.inet4Entries()
                    .map { (hostName, hostAddress) -> HostEntry(hostName, hostAddress) }
                    .associateBy { it.hostName }
            val ipv6 =
                nettyEntries.inet6Entries()
                    .map { (hostName, hostAddress) -> HostEntry(hostName, hostAddress) }
                    .associateBy { it.hostName }
            return HostEntries(ipv4, ipv6)
        }
    }

    fun generateHostFileContent(): String {
        val hostsFileEntries = this
        val content = StringBuilder()
        hostsFileEntries.ipv4.forEach { (hostName, hostEntry) ->
            content.append("${hostEntry.address.hostAddress}  ${hostName}\n")
        }
        content.append("\n\n")
        hostsFileEntries.ipv6.forEach { (hostName, hostEntry) ->
            content.append("${hostEntry.address.hostAddress}  ${hostName}\n")
        }
        return content.toString()
    }

    fun updateHostEntries(updatingHostEntry: HostEntry): HostEntries {
        val prevEntries = this
        val ipv4Entries = HashMap(prevEntries.ipv4)
        if (updatingHostEntry.ipv4) {
            ipv4Entries[updatingHostEntry.hostName] = updatingHostEntry
        }
        val ipv6Entries = HashMap(prevEntries.ipv6)
        if (updatingHostEntry.ipv6) {
            ipv6Entries[updatingHostEntry.hostName] = updatingHostEntry
        }

        return HostEntries(ipv4Entries, ipv6Entries)
    }

    fun removeHostEntries(removingHostEntry: HostEntry): HostEntries {
        val prevEntries = this
        val ipv4Entries = HashMap(prevEntries.ipv4)
        if (removingHostEntry.ipv4) {
            ipv4Entries.remove(removingHostEntry.hostName)
        }
        val ipv6Entries = HashMap(prevEntries.ipv6)
        if (removingHostEntry.ipv6) {
            ipv6Entries.remove(removingHostEntry.hostName)
        }
        return HostEntries(ipv4Entries, ipv6Entries)
    }
}