package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Optional LIVE audio levels for the in-app visualiser.
 *
 * Uses android.media.audiofx.Visualizer on the global output mix. It needs the
 * RECORD_AUDIO permission. If the permission is missing, the device blocks it, or
 * the playing app protects its audio, [start] returns false and the UI falls back
 * to the animated visualiser.
 */
object VisualizerEngine {
    const val BAR_COUNT = 24

    private var visualizer: Visualizer? = null
    private val smoothed = FloatArray(BAR_COUNT)

    private val _levels = MutableStateFlow(FloatArray(BAR_COUNT))
    val levels: StateFlow<FloatArray> = _levels.asStateFlow()

    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @Synchronized
    fun start(context: Context): Boolean {
        if (visualizer != null) return true
        if (!hasPermission(context)) return false
        return try {
            val v = Visualizer(0)
            v.enabled = false
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = min(range[1], 1024).coerceAtLeast(range[0])
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                    override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) processFft(fft)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            v.enabled = true
            visualizer = v
            _isLive.value = true
            true
        } catch (t: Throwable) {
            release()
            false
        }
    }

    @Synchronized
    fun stop() {
        release()
    }

    private fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (t: Throwable) {
            // ignore
        }
        visualizer = null
        _isLive.value = false
        java.util.Arrays.fill(smoothed, 0f)
        _levels.value = FloatArray(BAR_COUNT)
    }

    private fun processFft(fft: ByteArray) {
        // Layout: [dc, nyquist, re1, im1, re2, im2, ...]
        val bins = fft.size / 2 - 1
        if (bins < BAR_COUNT) return
        // Only use the lower ~60 % of the spectrum, that is where music energy is.
        val usable = (bins * 0.6f).toInt().coerceAtLeast(BAR_COUNT)
        val out = FloatArray(BAR_COUNT)
        for (bar in 0 until BAR_COUNT) {
            // Logarithmic-ish grouping: low bars cover few bins, high bars cover more.
            val startF = (usable * (bar.toFloat() / BAR_COUNT) * (bar.toFloat() / BAR_COUNT)).toInt()
            val endF = (usable * ((bar + 1).toFloat() / BAR_COUNT) * ((bar + 1).toFloat() / BAR_COUNT)).toInt()
            val from = max(1, startF)
            val to = max(from + 1, endF)
            var peak = 0f
            for (i in from until min(to, bins)) {
                val re = fft[2 * i].toFloat()
                val im = fft[2 * i + 1].toFloat()
                peak = max(peak, hypot(re, im))
            }
            // 0..~181 -> 0..1 with a soft curve
            out[bar] = sqrt((peak / 128f).coerceIn(0f, 1f))
        }
        for (i in 0 until BAR_COUNT) {
            // fast attack, slow decay = nicer bars
            smoothed[i] = if (out[i] > smoothed[i]) out[i] else smoothed[i] * 0.82f + out[i] * 0.18f
        }
        _levels.value = smoothed.copyOf()
    }
}
