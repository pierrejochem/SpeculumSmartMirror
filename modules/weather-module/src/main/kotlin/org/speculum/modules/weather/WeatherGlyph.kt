package org.speculum.modules.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Monochrome (white) weather icons drawn as vectors, so contrast is pure
 * black/white on a two-way mirror. Avoids color-emoji font fallback that
 * Compose Desktop applies to weather glyphs regardless of text presentation.
 */
@Composable
fun WeatherGlyph(code: Int, size: Dp, color: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = kotlin.math.min(this.size.width, this.size.height)
        val stroke = s * 0.06f
        when (code) {
            0 -> sun(color, stroke)                       // clear
            in 1..3 -> sunCloud(color, stroke)            // partly cloudy
            45, 48 -> fog(color, stroke)                  // fog
            in 51..67, in 80..82 -> rain(color, stroke)   // rain / showers
            in 71..77 -> snow(color, stroke)              // snow
            in 95..99 -> storm(color, stroke)             // thunderstorm
            else -> cloud(color, stroke, Offset(s * 0.5f, s * 0.5f), s * 0.42f)
        }
    }
}

// --- shape primitives -------------------------------------------------------

private fun DrawScope.sun(color: Color, stroke: Float, cx: Float = size.width * 0.5f,
                          cy: Float = size.height * 0.5f, r: Float = size.minDimension() * 0.22f) {
    drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(width = stroke))
    val rayIn = r * 1.4f
    val rayOut = r * 1.9f
    for (i in 0 until 8) {
        val a = (i * 45.0) * kotlin.math.PI / 180.0
        val dx = kotlin.math.cos(a).toFloat()
        val dy = kotlin.math.sin(a).toFloat()
        drawLine(color,
            Offset(cx + dx * rayIn, cy + dy * rayIn),
            Offset(cx + dx * rayOut, cy + dy * rayOut),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

private fun DrawScope.cloud(color: Color, stroke: Float, center: Offset, w: Float) {
    // Cloud as a rounded outline built from overlapping arcs.
    val h = w * 0.62f
    val left = center.x - w / 2
    val top = center.y - h / 2
    val path = Path().apply {
        val baseY = top + h
        moveTo(left + w * 0.12f, baseY)
        // bottom edge
        lineTo(left + w * 0.88f, baseY)
        // right bump
        cubicTo(left + w * 1.02f, baseY, left + w * 1.02f, top + h * 0.45f,
            left + w * 0.82f, top + h * 0.42f)
        // top-right lobe
        cubicTo(left + w * 0.86f, top + h * 0.05f, left + w * 0.5f, top - h * 0.05f,
            left + w * 0.42f, top + h * 0.28f)
        // top-left lobe
        cubicTo(left + w * 0.3f, top + h * 0.05f, left + w * 0.04f, top + h * 0.2f,
            left + w * 0.14f, top + h * 0.5f)
        // left bump back to base
        cubicTo(left - w * 0.04f, top + h * 0.55f, left - w * 0.02f, baseY,
            left + w * 0.12f, baseY)
        close()
    }
    drawPath(path, color, style = Stroke(width = stroke))
}

private fun DrawScope.sunCloud(color: Color, stroke: Float) {
    val s = size.minDimension()
    sun(color, stroke, cx = s * 0.36f, cy = s * 0.34f, r = s * 0.16f)
    cloud(color, stroke, Offset(s * 0.58f, s * 0.62f), s * 0.5f)
}

private fun DrawScope.fog(color: Color, stroke: Float) {
    val s = size.minDimension()
    cloud(color, stroke, Offset(s * 0.5f, s * 0.38f), s * 0.6f)
    for (i in 0..2) {
        val y = s * (0.66f + i * 0.13f)
        drawLine(color, Offset(s * 0.2f, y), Offset(s * 0.8f, y),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

private fun DrawScope.rain(color: Color, stroke: Float) {
    val s = size.minDimension()
    cloud(color, stroke, Offset(s * 0.5f, s * 0.36f), s * 0.62f)
    for (i in 0..2) {
        val x = s * (0.32f + i * 0.18f)
        drawLine(color, Offset(x, s * 0.66f), Offset(x - s * 0.06f, s * 0.9f),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

private fun DrawScope.snow(color: Color, stroke: Float) {
    val s = size.minDimension()
    cloud(color, stroke, Offset(s * 0.5f, s * 0.36f), s * 0.62f)
    for (i in 0..2) {
        val cx = s * (0.32f + i * 0.18f)
        val cy = s * 0.8f
        val d = s * 0.06f
        drawLine(color, Offset(cx - d, cy), Offset(cx + d, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy - d), Offset(cx, cy + d), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx - d * 0.7f, cy - d * 0.7f), Offset(cx + d * 0.7f, cy + d * 0.7f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx - d * 0.7f, cy + d * 0.7f), Offset(cx + d * 0.7f, cy - d * 0.7f),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

private fun DrawScope.storm(color: Color, stroke: Float) {
    val s = size.minDimension()
    cloud(color, stroke, Offset(s * 0.5f, s * 0.36f), s * 0.62f)
    val bolt = Path().apply {
        moveTo(s * 0.52f, s * 0.62f)
        lineTo(s * 0.4f, s * 0.82f)
        lineTo(s * 0.5f, s * 0.82f)
        lineTo(s * 0.42f, s * 0.98f)
        lineTo(s * 0.62f, s * 0.74f)
        lineTo(s * 0.52f, s * 0.74f)
        close()
    }
    drawPath(bolt, color)
}

private fun Size.minDimension(): Float = kotlin.math.min(width, height)