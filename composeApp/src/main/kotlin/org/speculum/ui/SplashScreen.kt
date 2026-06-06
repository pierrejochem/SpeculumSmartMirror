package org.speculum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bootstrap splash: title, a percentage + progress bar, and the rolling boot
 * log. Shown until the engine has loaded modules and fetched their first data,
 * then the app swaps to the mirror.
 */
@Composable
fun SplashScreen(messages: List<String>, progress: Float) {
    val pct = (progress.coerceIn(0f, 1f) * 100).toInt()
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Speculum", color = MirrorColors.Bright, fontSize = 44.sp, fontWeight = FontWeight.Thin)

            Text("$pct%", color = MirrorColors.Bright, fontSize = 22.sp,
                fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 20.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = MirrorColors.Bright,
                trackColor = MirrorColors.Dimmed,
                modifier = Modifier.padding(top = 10.dp).width(320.dp)
            )

            Column(
                Modifier.padding(top = 24.dp).width(420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val tail = messages.takeLast(4)
                tail.forEachIndexed { i, msg ->
                    val newest = i == tail.lastIndex
                    Text(
                        msg,
                        color = if (newest) MirrorColors.Normal else MirrorColors.Dimmed,
                        fontSize = if (newest) 15.sp else 13.sp
                    )
                }
            }
        }
    }
}