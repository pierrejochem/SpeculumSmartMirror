package org.speculum.modules.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Monochrome calendar icon drawn as a vector (page outline, top binding rings,
 * header bar) so it stays pure white on the mirror — no color-emoji fallback.
 */
@Composable
fun CalendarGlyph(size: Dp, color: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = kotlin.math.min(this.size.width, this.size.height)
        val stroke = s * 0.07f
        val left = s * 0.12f
        val right = s * 0.88f
        val top = s * 0.22f
        val bottom = s * 0.90f
        val w = right - left
        val h = bottom - top

        // Page body outline
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(s * 0.08f, s * 0.08f),
            style = Stroke(width = stroke)
        )
        // Header bar under the top edge
        val headerY = top + h * 0.26f
        drawLine(color, Offset(left, headerY), Offset(right, headerY),
            strokeWidth = stroke, cap = StrokeCap.Butt)
        // Two binding rings sticking above the top edge
        val ringTop = top - s * 0.10f
        val ringBottom = top + h * 0.10f
        drawLine(color, Offset(left + w * 0.28f, ringTop), Offset(left + w * 0.28f, ringBottom),
            strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(left + w * 0.72f, ringTop), Offset(left + w * 0.72f, ringBottom),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
}