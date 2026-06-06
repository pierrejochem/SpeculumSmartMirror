package org.speculum.configserver

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Lists the host's non-loopback IPv4 addresses (site-local first) so the admin
 * UI can offer a choice of which IP the QR code / URL should point to.
 */
fun localIPv4Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .filter { !it.isLoopbackAddress }
        .map { it.hostAddress }
        .distinct()
        .sortedByDescending { it.startsWith("192.168.") || it.startsWith("10.") || it.startsWith("172.") }
}.getOrDefault(emptyList())