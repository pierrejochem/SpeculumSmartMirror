package org.speculum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.speculum.core.MirrorEngine
import org.speculum.core.MirrorModule
import org.speculum.core.Region

/**
 * Lays modules into MagicMirror's classic region grid on a pure-black
 * background (so a two-way mirror shows only the bright text).
 *
 * Three screen-anchored layers, so the center group (compliments) stays
 * vertically centered on the display regardless of screen size:
 *  - top band   anchored to the top
 *  - center     centered in the full screen height
 *  - bottom     anchored to the bottom (band + news ticker)
 */
@Composable
fun MirrorScreen(engine: MirrorEngine) {
    val byRegion: Map<Region, List<MirrorModule>> = engine.modules.groupBy { it.region }
    fun region(r: Region) = byRegion[r].orEmpty()

    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        // Top band: left / center / right, each stacking its modules. Natural height.
        RegionBand(
            start = region(Region.TOP_LEFT),
            center = region(Region.TOP_CENTER),
            end = region(Region.TOP_RIGHT)
        )

        // Center group — centered in the space left between top and bottom, so
        // it tracks screen size yet never overlaps the bands on resize.
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val centered = region(Region.UPPER_THIRD) + region(Region.MIDDLE_CENTER) +
                region(Region.LOWER_THIRD)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                centered.forEach { Slot(it) }
            }
        }

        // Bottom band: left / center / right. Natural height.
        RegionBand(
            start = region(Region.BOTTOM_LEFT),
            center = region(Region.BOTTOM_CENTER),
            end = region(Region.BOTTOM_RIGHT)
        )
        // Persistent news ticker, centered full width. Natural height.
        region(Region.BOTTOM_BAR).takeIf { it.isNotEmpty() }?.let { bar ->
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { bar.forEach { Slot(it) } }
        }
    }
}

@Composable
private fun RegionBand(
    start: List<MirrorModule>,
    center: List<MirrorModule>,
    end: List<MirrorModule>
) {
    if (start.isEmpty() && center.isEmpty() && end.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.Start) { start.forEach { Slot(it) } }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { center.forEach { Slot(it) } }
        Column(horizontalAlignment = Alignment.End) { end.forEach { Slot(it) } }
    }
}

@Composable
private fun Slot(module: MirrorModule) {
    Box(Modifier.padding(8.dp)) { module.Content() }
}