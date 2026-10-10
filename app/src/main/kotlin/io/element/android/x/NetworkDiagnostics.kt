package io.element.android.x

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory
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
                probeServer("syncme.ir")
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
                            probeServer("syncme.ir")
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
    /** Separate non-authenticated probes. These never change the Matrix client transport. */
    private fun probeServer(host: String) {
        val started = SystemClock.elapsedRealtime()
        val addresses = runCatching { InetAddress.getAllByName(host) }.getOrElse {
            StartupTrace.mark("probe_dns_failed_" + it.javaClass.simpleName + "_" + (it.message ?: "").take(70))
            return
        }
        StartupTrace.mark("probe_dns_ok_ms_" + (SystemClock.elapsedRealtime() - started) + "_addresses_" + addresses.size)
        addresses.take(2).forEach { address ->
            val connectAt = SystemClock.elapsedRealtime()
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(address, 443), 3500)
                    StartupTrace.mark("probe_tcp_443_ok_ms_" + (SystemClock.elapsedRealtime() - connectAt) + "_ip_" + address.hostAddress)
                    socket.soTimeout = 3500
                    val tlsAt = SystemClock.elapsedRealtime()
                    (SSLSocketFactory.getDefault().createSocket(socket, host, 443, true) as javax.net.ssl.SSLSocket).use { tls ->
                        tls.soTimeout = 3500
                        tls.startHandshake()
                        StartupTrace.mark("probe_tls_ok_ms_" + (SystemClock.elapsedRealtime() - tlsAt) + "_protocol_" + tls.session.protocol)
                    }
                }
            }.onFailure {
                StartupTrace.mark("probe_tcp_tls_failed_" + it.javaClass.simpleName + "_" + (it.message ?: "").take(90))
            }
        }
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
