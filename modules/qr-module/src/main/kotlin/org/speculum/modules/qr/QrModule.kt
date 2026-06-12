package org.speculum.modules.qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import org.speculum.ui.MirrorColors
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Shows a QR code linking to the config web admin, so you can scan it with a
 * phone to manage the mirror. Rendered white-on-black (inverted) to match the
 * mirror — modern phone cameras read inverted QR codes.
 *
 * Config keys:
 *  - url   : full link to encode (overrides everything below)
 *  - ip    : host IP for the default url (default: auto-detected LAN IP)
 *  - port  : admin port for the default url (default 8080)
 *  - label : caption under the code (default "Scan to configure")
 *  - size  : code size in dp (default 110)
 */
class QrModule(config: ModuleConfig) : MirrorModule(config) {

    private val url = config.string("url", "").ifBlank {
        val ip = config.string("ip", "").ifBlank { lanIp() ?: "localhost" }
        "http://$ip:${config.int("port", 8080)}"
    }
    private val label = config.string("label", "Scan to configure")
    private val sizeDp = config.int("size", 110).coerceIn(60, 400).dp

    private val matrix: BitMatrix? = runCatching {
        QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 256, 256, mapOf(EncodeHintType.MARGIN to 1))
    }.getOrNull()

    @Composable
    override fun Content() {
        val m = matrix ?: return
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(sizeDp)) {
                val n = m.width
                val cell = size.width / n
                for (y in 0 until n) {
                    for (x in 0 until n) {
                        if (m.get(x, y)) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(x * cell, y * cell),
                                size = Size(cell, cell)
                            )
                        }
                    }
                }
            }
            if (label.isNotBlank()) Text(label, color = MirrorColors.Normal, fontSize = 12.sp)
        }
    }

    /** First non-loopback site-local IPv4 address, for a LAN-reachable URL. */
    private fun lanIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}