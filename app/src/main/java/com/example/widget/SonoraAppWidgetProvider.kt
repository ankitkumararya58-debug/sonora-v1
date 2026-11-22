package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.CustomArtworkManager
import com.example.data.SonoraPreferences
import com.example.model.CoverShape
import com.example.model.MediaTrackState
import com.example.model.WidgetPreset
import com.example.model.WidgetSettings
import com.example.model.WidgetTheme
import com.example.service.MediaPlaybackManager
import com.example.service.OutputDevice
import com.example.service.OutputKind

class SonoraAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                MediaPlaybackManager.togglePlayPause()
                updateAllWidgets(context)
            }
            ACTION_NEXT -> {
                MediaPlaybackManager.skipToNext()
                updateAllWidgets(context)
            }
            ACTION_PREVIOUS -> {
                MediaPlaybackManager.skipToPrevious()
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.sonora.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.sonora.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.sonora.ACTION_PREVIOUS"

        private val INK = Color.parseColor("#12151B")
        private val DEFAULT_PLAY_ICON = Color.parseColor("#1B2433")

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            try {
                val settings = SonoraPreferences(context).loadSettings()
                val trackState = MediaPlaybackManager.trackState.value
                val isCard = settings.preset == WidgetPreset.EXPANDED_CARD
                val isDefault = settings.theme == WidgetTheme.DEFAULT

                val layoutId = when {
                    isDefault && isCard -> R.layout.widget_sonora_card_default
                    isDefault -> R.layout.widget_sonora_pill_default
                    isCard -> R.layout.widget_sonora_card
                    else -> R.layout.widget_sonora_pill
                }
                val views = RemoteViews(context.packageName, layoutId)
                val metrics = WidgetRenderer.metricsFor(context, appWidgetManager, widgetId, if (isCard) 120 else 64)

                val customCover = if (settings.useCustomArtwork) {
                    CustomArtworkManager.loadCustomCoverBitmap(context)
                } else null
                val art = customCover ?: trackState.albumArt

                views.setTextViewText(R.id.widget_title, trackState.title)
                views.setTextViewText(R.id.widget_artist, trackState.artist)

                if (isDefault) {
                    buildDefault(context, views, settings, trackState, art, metrics, isCard)
                } else {
                    buildStandard(context, views, settings, trackState, art, metrics, isCard)
                }

                // Pending intents for controls
                views.setOnClickPendingIntent(
                    R.id.widget_btn_play_pause,
                    getBroadcastIntent(context, ACTION_PLAY_PAUSE, widgetId)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_next,
                    getBroadcastIntent(context, ACTION_NEXT, widgetId)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_prev,
                    getBroadcastIntent(context, ACTION_PREVIOUS, widgetId)
                )

                // Launch Sonora app when tapping widget root
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (t: Throwable) {
                // never crash the process because of a widget
                t.printStackTrace()
            }
        }

        // ------------------------------------------------------------------
        // DEFAULT theme: cover fills the whole widget, light chip, small buttons
        // ------------------------------------------------------------------
        private fun buildDefault(
            context: Context,
            views: RemoteViews,
            settings: WidgetSettings,
            trackState: MediaTrackState,
            art: Bitmap?,
            metrics: WidgetMetrics,
            isCard: Boolean
        ) {
            val radius = if (isCard) {
                settings.cornerRadiusDp.coerceAtLeast(16) * metrics.density
            } else {
                metrics.height / 2f
            }
            views.setImageViewBitmap(
                R.id.widget_bg,
                WidgetRenderer.defaultBackground(context, art, metrics.width, metrics.height, radius)
            )

            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (trackState.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            views.setImageViewResource(R.id.widget_btn_prev, R.drawable.ic_widget_prev)
            views.setImageViewResource(R.id.widget_btn_next, R.drawable.ic_widget_next)
            views.setInt(R.id.widget_btn_play_pause, "setColorFilter", DEFAULT_PLAY_ICON)
            views.setInt(R.id.widget_btn_prev, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widget_btn_next, "setColorFilter", Color.WHITE)

            if (!isCard) return

            // top-right: where the sound is going
            if (settings.showDeviceChip) {
                views.setTextViewText(
                    R.id.widget_device_chip,
                    deviceLabel(MediaPlaybackManager.outputDevice.value, withEmoji = false)
                )
                views.setViewVisibility(R.id.widget_device_chip, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_device_chip, View.GONE)
            }

            // top-left: source app icon
            val icon = WidgetRenderer.appIconCircle(
                context,
                trackState.packageName,
                (24 * metrics.density).toInt()
            )
            if (icon != null) {
                views.setImageViewBitmap(R.id.widget_source_icon, icon)
                views.setViewVisibility(R.id.widget_source_icon, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_source_icon, View.GONE)
            }

            // bottom: thin progress line between previous / next
            views.setImageViewBitmap(
                R.id.widget_progress,
                WidgetRenderer.progressLineBitmap(progressFraction(trackState).coerceAtLeast(0f), Color.WHITE)
            )
        }

        // ------------------------------------------------------------------
        // DARK / LIGHT / GLASS / PIXEL
        // ------------------------------------------------------------------
        private fun buildStandard(
            context: Context,
            views: RemoteViews,
            settings: WidgetSettings,
            trackState: MediaTrackState,
            art: Bitmap?,
            metrics: WidgetMetrics,
            isCard: Boolean
        ) {
            val theme = settings.theme
            val isPixel = theme == WidgetTheme.PIXEL
            val isGlass = theme == WidgetTheme.GLASS
            val isLight = theme == WidgetTheme.LIGHT

            // Background
            if (isPixel) {
                views.setInt(R.id.widget_root, "setBackgroundResource", 0)
                views.setImageViewBitmap(
                    R.id.widget_bg,
                    WidgetRenderer.pixelBackground(metrics.width, metrics.height, 3f * metrics.density)
                )
                views.setViewVisibility(R.id.widget_bg, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_bg, View.GONE)
                val backgroundRes = when (theme) {
                    WidgetTheme.LIGHT -> if (isCard) R.drawable.widget_card_background_light else R.drawable.widget_pill_background_light
                    WidgetTheme.GLASS -> if (isCard) R.drawable.widget_card_background_glass else R.drawable.widget_pill_background_glass
                    else -> if (isCard) R.drawable.widget_card_background_dark else R.drawable.widget_pill_background_dark
                }
                views.setInt(R.id.widget_root, "setBackgroundResource", backgroundRes)
            }

            // Text colors
            val titleColor = if (isLight || isPixel) INK else Color.WHITE
            val artistColor = when {
                isPixel -> Color.parseColor("#444444")
                isLight -> Color.parseColor("#5B6472")
                isGlass -> Color.parseColor("#D9E2EE")
                else -> Color.parseColor("#A0A8B5")
            }
            views.setTextColor(R.id.widget_title, titleColor)
            views.setTextColor(R.id.widget_artist, artistColor)

            // Cover: always a rounded rectangle (pixel-art with stepped corners on the Pixel theme)
            val artPx = ((if (isCard) 68 else 44) * metrics.density).toInt().coerceAtLeast(16)
            views.setImageViewBitmap(
                R.id.widget_album_art,
                WidgetRenderer.shapedCover(
                    context = context,
                    art = art,
                    shape = if (isPixel) CoverShape.PIXEL else CoverShape.ROUNDED,
                    widthPx = artPx,
                    heightPx = artPx,
                    sizePercent = 100,
                    cornerPercent = 18
                )
            )

            // Control icons + button backgrounds (themed)
            val pixelStyled = isPixel && settings.themedIcons
            val glassStyled = isGlass && settings.themedIcons

            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (trackState.isPlaying) {
                    if (pixelStyled) R.drawable.ic_pixel_pause else R.drawable.ic_widget_pause
                } else {
                    if (pixelStyled) R.drawable.ic_pixel_play else R.drawable.ic_widget_play
                }
            )
            views.setImageViewResource(
                R.id.widget_btn_prev,
                if (pixelStyled) R.drawable.ic_pixel_prev else R.drawable.ic_widget_prev
            )
            views.setImageViewResource(
                R.id.widget_btn_next,
                if (pixelStyled) R.drawable.ic_pixel_next else R.drawable.ic_widget_next
            )

            val baseTint = if (isLight || isPixel) INK else Color.WHITE
            val playTint = if (pixelStyled) Color.WHITE else baseTint
            views.setInt(R.id.widget_btn_play_pause, "setColorFilter", playTint)
            views.setInt(R.id.widget_btn_prev, "setColorFilter", baseTint)
            views.setInt(R.id.widget_btn_next, "setColorFilter", baseTint)

            val mainBg = when {
                pixelStyled -> R.drawable.widget_btn_pixel_main
                glassStyled -> R.drawable.widget_btn_glass
                else -> R.drawable.widget_btn_ripple
            }
            val smallBg = when {
                pixelStyled -> R.drawable.widget_btn_pixel_small
                glassStyled -> R.drawable.widget_btn_glass
                else -> R.drawable.widget_btn_ripple
            }
            views.setInt(R.id.widget_btn_play_pause, "setBackgroundResource", mainBg)
            views.setInt(R.id.widget_btn_prev, "setBackgroundResource", smallBg)
            views.setInt(R.id.widget_btn_next, "setBackgroundResource", smallBg)

            // Badges + visualiser exist only in the card layout
            if (!isCard) return

            if (settings.showDeviceChip) {
                views.setTextViewText(
                    R.id.widget_device_chip,
                    deviceLabel(MediaPlaybackManager.outputDevice.value, withEmoji = true)
                )
                views.setTextColor(R.id.widget_device_chip, if (isLight || isPixel) INK else Color.WHITE)
                views.setInt(
                    R.id.widget_device_chip,
                    "setBackgroundResource",
                    when {
                        isPixel -> R.drawable.widget_chip_pixel
                        isLight -> R.drawable.widget_chip_light
                        else -> R.drawable.widget_chip_dark
                    }
                )
                views.setViewVisibility(R.id.widget_device_chip, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_device_chip, View.GONE)
            }

            // Pixel theme: small source-app icon on the cover corner
            val icon = if (isPixel) {
                WidgetRenderer.appIconCircle(context, trackState.packageName, (20 * metrics.density).toInt())
            } else null
            if (icon != null) {
                views.setImageViewBitmap(R.id.widget_source_icon, icon)
                views.setViewVisibility(R.id.widget_source_icon, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_source_icon, View.GONE)
            }

            if (settings.showVisualizer) {
                views.setImageViewBitmap(
                    R.id.widget_visualizer,
                    WidgetRenderer.progressBarsBitmap(
                        seedText = trackState.title + trackState.artist,
                        progress = progressFraction(trackState),
                        color = if (isLight || isPixel) INK else Color.WHITE,
                        pixelStyle = isPixel,
                        dimmed = !trackState.isPlaying
                    )
                )
                views.setViewVisibility(R.id.widget_visualizer, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_visualizer, View.GONE)
            }
        }

        /** 0..1 how far the song is, or -1 when the app doesn't tell us the length. */
        private fun progressFraction(trackState: MediaTrackState): Float {
            val duration = trackState.durationMs
            if (duration <= 0L) return -1f
            val position = MediaPlaybackManager.currentPositionMs()
            return (position.toFloat() / duration).coerceIn(0f, 1f)
        }

        private fun deviceLabel(device: OutputDevice, withEmoji: Boolean): String {
            return if (withEmoji) {
                when (device.kind) {
                    OutputKind.PHONE_SPEAKER -> "\uD83D\uDD0A Speaker"
                    OutputKind.WIRED -> "\uD83C\uDFA7 Earphones"
                    OutputKind.BLUETOOTH -> "\uD83C\uDFA7 " + device.label
                    OutputKind.USB -> "\uD83C\uDFA7 USB audio"
                    OutputKind.OTHER -> "\uD83C\uDFA7 " + device.label
                }
            } else {
                when (device.kind) {
                    OutputKind.PHONE_SPEAKER -> "This phone"
                    OutputKind.WIRED -> "Earphones"
                    OutputKind.USB -> "USB audio"
                    else -> device.label
                }
            }
        }

        private fun getBroadcastIntent(context: Context, action: String, widgetId: Int): PendingIntent {
            val intent = Intent(context, SonoraAppWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId xor action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SonoraAppWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, appWidgetManager, id)
            }
            SonoraCoverWidgetProvider.updateAll(context)
        }
    }
}
