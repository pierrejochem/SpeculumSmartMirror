package org.speculum.modules.compliments

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.speculum.config.ModuleConfig
import org.speculum.core.MirrorModule
import kotlin.random.Random

/**
 * Port of MagicMirror's default `compliments` module.
 *
 * Config keys (mirroring compliments' options):
 *  - updateInterval     : seconds between rotations (default 30)
 *  - fadeSpeed          : crossfade duration in ms (default 4000)
 *  - random             : random vs. sequential order (default true)
 *  - morningStartTime   : hour morning begins (default 3)
 *  - morningEndTime     : hour morning ends   (default 12)
 *  - afternoonStartTime : hour afternoon begins (default 12)
 *  - afternoonEndTime   : hour afternoon ends   (default 17)
 *  - specialDayUnique   : a matching date entry replaces (not extends) the pool (default false)
 *  - compliments        : JSON object of pools, e.g.
 *                         {"morning":[...],"anytime":[...],"....-12-25":["Merry Christmas!"]}
 *  - remoteFile         : URL to a JSON file with the same shape (overrides `compliments`)
 *
 * Pools are keyed by "morning"/"afternoon"/"evening"/"anytime" plus optional
 * date-regex keys (e.g. "....-12-25", "....-..-01") tested against today's date.
 */
class ComplimentsModule(config: ModuleConfig) : MirrorModule(config) {

    private val rotateMs = config.int("updateInterval", 30).coerceAtLeast(1) * 1000L
    private val fadeSpeed = config.int("fadeSpeed", 4000).coerceAtLeast(0)
    private val random = config.bool("random", true)
    private val morningStart = config.int("morningStartTime", 3)
    private val morningEnd = config.int("morningEndTime", 12)
    private val afternoonStart = config.int("afternoonStartTime", 12)
    private val afternoonEnd = config.int("afternoonEndTime", 17)
    private val specialDayUnique = config.bool("specialDayUnique", false)
    private val remoteFile = config.string("remoteFile", "")

    private val json = Json { ignoreUnknownKeys = true }
    private var pools: Map<String, List<String>> =
        config.string("compliments", "").takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<Map<String, List<String>>>(it) }.getOrNull() }
            ?: DEFAULTS

    private var lastIndex = -1
    private var text by mutableStateOf("")

    override fun start(scope: CoroutineScope) {
        scope.launch {
            if (remoteFile.isNotBlank()) loadRemote()
            while (true) {
                text = pick()
                delay(rotateMs)
            }
        }
    }

    private suspend fun loadRemote() {
        runCatching {
            val body = HttpClient().get(remoteFile).bodyAsText()
            json.decodeFromString<Map<String, List<String>>>(body)
        }.onSuccess { pools = it }
    }

    /** Mirrors compliments.js complimentArray(): build today's pool. */
    private fun complimentArray(): List<String> {
        val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = dt.hour
        val date = "${dt.year}-${pad(dt.monthNumber)}-${pad(dt.dayOfMonth)}"

        var list = when {
            hour in morningStart until morningEnd && pools.containsKey("morning") ->
                pools.getValue("morning").toMutableList()
            hour in afternoonStart until afternoonEnd && pools.containsKey("afternoon") ->
                pools.getValue("afternoon").toMutableList()
            pools.containsKey("evening") -> pools.getValue("evening").toMutableList()
            else -> mutableListOf()
        }

        pools["anytime"]?.let { list += it }

        // Date-based entries: keys are regexes tested against today's date.
        for ((key, values) in pools) {
            if (key in STANDARD) continue
            val matches = runCatching { Regex(key).containsMatchIn(date) }.getOrDefault(false)
            if (matches) {
                if (specialDayUnique) list = values.toMutableList() else list += values
            }
        }
        return list
    }

    private fun pick(): String {
        val pool = complimentArray()
        if (pool.isEmpty()) return text
        val i = nextIndex(pool.size)
        return pool[i]
    }

    /** Random with no immediate repeat, or sequential when `random` is false. */
    private fun nextIndex(size: Int): Int {
        if (size <= 1) { lastIndex = 0; return 0 }
        return if (random) {
            var i = Random.nextInt(size)
            while (i == lastIndex) i = Random.nextInt(size)
            lastIndex = i
            i
        } else {
            lastIndex = (lastIndex + 1) % size
            lastIndex
        }
    }

    @Composable
    override fun Content() {
        // Fade fully out, swap the message, fade back in (like MagicMirror) —
        // never show two messages at once.
        val alpha = remember { Animatable(0f) }
        var shown by remember { mutableStateOf(text) }
        val half = (fadeSpeed / 2).coerceAtLeast(0)
        LaunchedEffect(text) {
            if (shown.isNotEmpty()) alpha.animateTo(0f, tween(half))
            shown = text
            alpha.animateTo(1f, tween(half))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                shown,
                color = Color.White,
                fontSize = 60.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')

    private companion object {
        val STANDARD = setOf("morning", "afternoon", "evening", "anytime")
        val DEFAULTS = mapOf(
            "morning" to listOf("Good morning, handsome!", "Enjoy your day!", "How was your sleep?"),
            "afternoon" to listOf("Hello, beauty!", "You look sexy!", "Looking good today!"),
            "evening" to listOf("Wow, you look hot!", "You look nice!", "Hi, sexy!"),
        )
    }
}