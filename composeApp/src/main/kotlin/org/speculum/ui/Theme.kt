package org.speculum.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import org.speculum.resources.Res
import org.speculum.resources.roboto_light
import org.speculum.resources.roboto_medium
import org.speculum.resources.roboto_regular
import org.speculum.resources.roboto_thin

/** Roboto, the font MagicMirror ships (Thin for the clock, Light for body). */
@Composable
fun robotoFamily(): FontFamily = FontFamily(
    Font(Res.font.roboto_thin, FontWeight.Thin),
    Font(Res.font.roboto_light, FontWeight.Light),
    Font(Res.font.roboto_regular, FontWeight.Normal),
    Font(Res.font.roboto_medium, FontWeight.Medium),
)