package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import com.example.service.VisualizerEngine
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Spotify-style bar visualiser.
 *
 * - liveEnabled + RECORD_AUDIO granted -> real FFT levels from the audio output mix
 * - otherwise -> smooth animated bars (seeded by the song so every song "looks" different)
 */
@Composable
fun AudioVisualizerBars(
    isPlaying: Boolean,
    seed: String,
    color: Color,
    modifier: Modifier = Modifier,
    pixelStyle: Boolean = false,
    liveEnabled: Boolean = false
) {
    val context = LocalContext.current
    val barCount = VisualizerEngine.BAR_COUNT

    var liveOk by remember { mutableStateOf(false) }
    DisposableEffect(liveEnabled, isPlaying) {
        liveOk = if (liveEnabled && isPlaying) VisualizerEngine.start(context) else false
        onDispose {
            if (liveEnabled) VisualizerEngine.stop()
        }
    }
    val liveLevels by VisualizerEngine.levels.collectAsState()

    var frameTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying, liveOk) {
        if (isPlaying && !liveOk) {
            while (true) {
                withFrameNanos { frameTime = it }
            }
        }
    }

    // stable per-song "personality" for the animated fallback
    val speeds = remember(seed) {
        val rnd = java.util.Random(seed.hashCode().toLong())
        FloatArray(barCount * 4) { rnd.nextFloat() }
    }

    Canvas(modifier = modifier) {
        val slot = size.width / barCount
        val barWidth = slot * 0.55f
        val t = frameTime / 1_000_000_000f
        for (i in 0 until barCount) {
            val level = when {
                !isPlaying -> 0.08f
                liveOk -> liveLevels.getOrElse(i) { 0f }.coerceIn(0.05f, 1f)
                else -> {
                    val a = (sin(t * (1.4f + speeds[i * 4] * 3.2f) + speeds[i * 4 + 1] * 6.28f) + 1f) / 2f
                    val b = (sin(t * (0.7f + speeds[i * 4 + 2] * 2.4f) + speeds[i * 4 + 3] * 6.28f) + 1f) / 2f
                    (0.12f + 0.88f * (a * b).pow(0.7f)).coerceIn(0.05f, 1f)
                }
            }
            var h = size.height * level
            if (pixelStyle) {
                val step = size.height / 8f
                h = (h / step).roundToInt().coerceAtLeast(1) * step
            }
            val x = slot * i + slot / 2f
            val top = (size.height - h) / 2f
            if (pixelStyle) {
                drawRect(
                    color = color,
                    topLeft = Offset(x - barWidth / 2f, top),
                    size = Size(barWidth, h)
                )
            } else {
                val inset = barWidth / 2f
                drawLine(
                    color = color,
                    start = Offset(x, top + inset),
                    end = Offset(x, (top + h - inset).coerceAtLeast(top + inset)),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
