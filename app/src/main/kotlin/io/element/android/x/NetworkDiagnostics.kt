package io.element.android.x

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Process
import io.element.android.libraries.matrix.api.diagnostics.StartupTrace
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Reports network configuration and UID traffic, not packet destinations. */
internal object NetworkDiagnostics {
    private val started = AtomicBoolean(false)
    private val io = Executors.newSingleThreadExecutor { task ->
        Thread(task, "syncme-network-diag").apply { isDaemon = true }
    }
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val uid = Process.myUid()
        StartupTrace.mark("net_monitor_started")
        io.execute {
            runCatching {
                cm.activeNetwork?.let { net ->
                    StartupTrace.mark("net_active_" + net.toString())
                    cm.getLinkProperties(net)?.let(::recordLink)
                    cm.getNetworkCapabilities(net)?.let(::recordCapabilities)
                }
                recordBytes(uid)
            }.onFailure { StartupTrace.mark("net_initial_error_" + it.javaClass.simpleName) }
        }
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    StartupTrace.mark("net_available_" + network.toString())
                    io.execute {
                        runCatching {
                            cm.getLinkProperties(network)?.let(::recordLink)
                            cm.getNetworkCapabilities(network)?.let(::recordCapabilities)
                            recordBytes(uid)
                        }
                    }
                }
                override fun onLost(network: Network) {
                    StartupTrace.mark("net_lost_" + network.toString())
                    io.execute { recordBytes(uid) }
                }
                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    io.execute { recordLink(linkProperties) }
                }
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    io.execute { recordCapabilities(networkCapabilities) }
                }
            })
        }.onFailure { StartupTrace.mark("net_register_error_" + it.javaClass.simpleName) }
    }
    private fun recordBytes(uid: Int) {
        StartupTrace.mark("net_uid_tx_" + TrafficStats.getUidTxBytes(uid) + "_rx_" + TrafficStats.getUidRxBytes(uid))
    }
    private fun recordLink(link: LinkProperties) {
        StartupTrace.mark("net_interface_" + (link.interfaceName ?: "unknown"))
        link.dnsServers.forEach { StartupTrace.mark("net_configured_dns_" + it.hostAddress.orEmpty()) }
        link.linkAddresses.forEach { StartupTrace.mark("net_local_address_" + it.address.hostAddress.orEmpty()) }
    }
    private fun recordCapabilities(capabilities: NetworkCapabilities) {
        StartupTrace.mark("net_using_vpn_" + capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        StartupTrace.mark("net_internet_capability_" + capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
    }
}
