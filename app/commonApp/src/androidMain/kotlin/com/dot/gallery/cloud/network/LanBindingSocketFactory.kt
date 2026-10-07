/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import javax.net.SocketFactory
import com.dot.gallery.domain.uri.host
import com.dot.gallery.core.logging.printDebug


/**
 * A [SocketFactory] that binds sockets destined for a private/LAN address to the device's
 * Wi-Fi/Ethernet network.
 *
 * Why: when the Wi-Fi that hosts a self-hosted server (e.g. Immich on `192.168.x.x`) has no
 * internet access, Android keeps mobile data as the process's *default* network. OkHttp's
 * sockets then follow the default network and connection attempts to the LAN IP are silently
 * black-holed (30s connect timeout) — even though a browser, which explicitly selects Wi-Fi
 * for a private-range host, reaches the server fine. Binding the socket to the local Wi-Fi
 * network reproduces that browser behaviour.
 *
 * Only genuinely local destinations (RFC-1918 site-local, loopback, link-local) are bound;
 * every other connection is left on the default network, so external/public URLs and
 * VPN/tunnel (e.g. Tailscale CGNAT) traffic are unaffected. A no-op when no Wi-Fi/Ethernet
 * network is available.
 */
class LanBindingSocketFactory(
    private val context: Context
) : SocketFactory() {

    private fun localNetwork(): Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        return try {
            @Suppress("DEPRECATION")
            cm.allNetworks.firstOrNull { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@firstOrNull false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        } catch (_: SecurityException) {
            // ACCESS_NETWORK_STATE unavailable; leave the socket on the default network.
            null
        }
    }

    private fun InetAddress.isLan(): Boolean =
        isSiteLocalAddress || isLoopbackAddress || isLinkLocalAddress

    private inner class BindingSocket : Socket() {
        override fun connect(endpoint: SocketAddress?, timeout: Int) {
            val address = (endpoint as? InetSocketAddress)?.address
            if (address != null && address.isLan()) {
                localNetwork()?.let { network ->
                    runCatching { network.bindSocket(this) }
                        .onSuccess { printDebug("LanBindingSocketFactory: bound socket to Wi-Fi for $address") }
                }
            }
            super.connect(endpoint, timeout)
        }
    }

    // OkHttp uses the no-arg overload: it creates the socket, then connects it (which is where
    // the LAN binding happens). The connecting overloads are provided for completeness.
    override fun createSocket(): Socket = BindingSocket()

    override fun createSocket(host: String?, port: Int): Socket =
        BindingSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        BindingSocket().apply {
            bind(InetSocketAddress(localHost, localPort))
            connect(InetSocketAddress(host, port))
        }

    override fun createSocket(host: InetAddress?, port: Int): Socket =
        BindingSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        BindingSocket().apply {
            bind(InetSocketAddress(localAddress, localPort))
            connect(InetSocketAddress(address, port))
        }
}
