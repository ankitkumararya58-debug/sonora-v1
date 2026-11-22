package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.CustomArtworkManager
import com.example.data.SonoraPreferences
import com.example.model.WidgetPreset
import com.example.model.WidgetTheme
import com.example.service.MediaPlaybackManager
import com.example.service.OutputKind

class SonoraAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
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

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val prefs = SonoraPreferences(context)
            val settings = prefs.loadSettings()
            val trackState = MediaPlaybackManager.trackState.value

            val isCard = settings.preset == WidgetPreset.EXPANDED_CARD
            val layoutId = if (isCard) R.layout.widget_sonora_card else R.layout.widget_sonora_pill

            val views = RemoteViews(context.packageName, layoutId)

            val theme = settings.theme
            val isPixel = theme == WidgetTheme.PIXEL
            val isGlass = theme == WidgetTheme.GLASS
            val isLight = theme == WidgetTheme.LIGHT

            // Background selection
            val backgroundRes = when (theme) {
                WidgetTheme.DARK -> if (isCard) R.drawable.widget_card_background_dark else R.drawable.widget_pill_background_dark
                WidgetTheme.LIGHT -> if (isCard) R.drawable.widget_card_background_light else R.drawable.widget_pill_background_light
                WidgetTheme.TRANSPARENT -> if (isCard) R.drawable.widget_card_background_trans else R.drawable.widget_pill_background_trans
                WidgetTheme.PIXEL -> if (isCard) R.drawable.widget_card_background_pixel else R.drawable.widget_pill_background_pixel
                WidgetTheme.GLASS -> if (isCard) R.drawable.widget_card_background_glass else R.drawable.widget_pill_background_glass
            }
            views.setInt(R.id.widget_root, "setBackgroundResource", backgroundRes)

            // Text colors
            val titleColor = when {
                isLight || isPixel -> Color.parseColor("#12151B")
                else -> Color.parseColor("#FFFFFF")
            }
            val artistColor = when {
                isPixel -> Color.parseColor("#444444")
                isLight -> Color.parseColor("#5B6472")
                isGlass -> Color.parseColor("#D9E2EE")
                else -> Color.parseColor("#A0A8B5")
            }

            views.setTextColor(R.id.widget_title, titleColor)
            views.setTextColor(R.id.widget_artist, artistColor)

            // Song metadata
            views.setTextViewText(R.id.widget_title, trackState.title)
            views.setTextViewText(R.id.widget_artist, trackState.artist)

            // Artwork
            val customCover = if (settings.useCustomArtwork) {
                CustomArtworkManager.loadCustomCoverBitmap(context)
            } else null

            val displayArt = customCover ?: trackState.albumArt
            if (displayArt != null) {
                views.setImageViewBitmap(R.id.widget_album_art, displayArt)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music_default)
            }

            // Control icons + button backgrounds (themed)
            val usePixelIcons = settings.themedIcons && isPixel
            val playPauseRes = if (trackState.isPlaying) {
                if (usePixelIcons) R.drawable.ic_pixel_pause else R.drawable.ic_widget_pause
            } else {
                if (usePixelIcons) R.drawable.ic_pixel_play else R.drawable.ic_widget_play
            }
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseRes)
            views.setImageViewResource(
                R.id.widget_btn_prev,
                if (usePixelIcons) R.drawable.ic_pixel_prev else R.drawable.ic_widget_prev
            )
            views.setImageViewResource(
                R.id.widget_btn_next,
                if (usePixelIcons) R.drawable.ic_pixel_next else R.drawable.ic_widget_next
            )

            // The stock icons are white; tint them so they stay visible on light / pixel themes.
            val iconTint = if (isLight || isPixel) Color.parseColor("#12151B") else Color.WHITE
            views.setInt(R.id.widget_btn_play_pause, "setColorFilter", iconTint)
            views.setInt(R.id.widget_btn_prev, "setColorFilter", iconTint)
            views.setInt(R.id.widget_btn_next, "setColorFilter", iconTint)

            if (settings.themedIcons && (isPixel || isGlass)) {
                val mainBg = if (isPixel) R.drawable.widget_btn_pixel_main else R.drawable.widget_btn_glass
                val smallBg = if (isPixel) R.drawable.widget_btn_pixel_small else R.drawable.widget_btn_glass
                views.setInt(R.id.widget_btn_play_pause, "setBackgroundResource", mainBg)
                views.setInt(R.id.widget_btn_prev, "setBackgroundResource", smallBg)
                views.setInt(R.id.widget_btn_next, "setBackgroundResource", smallBg)
            } else {
                views.setInt(R.id.widget_btn_play_pause, "setBackgroundResource", R.drawable.widget_btn_ripple)
                views.setInt(R.id.widget_btn_prev, "setBackgroundResource", R.drawable.widget_btn_ripple)
                views.setInt(R.id.widget_btn_next, "setBackgroundResource", R.drawable.widget_btn_ripple)
            }

            // Output device badge + visualiser strip exist only in the card layout
            if (isCard) {
                if (settings.showDeviceChip) {
                    val device = MediaPlaybackManager.outputDevice.value
                    val label = when (device.kind) {
                        OutputKind.PHONE_SPEAKER -> "\uD83D\uDD0A Speaker"
                        OutputKind.WIRED -> "\uD83C\uDFA7 Earphones"
                        OutputKind.BLUETOOTH -> "\uD83C\uDFA7 " + device.label
                        OutputKind.USB -> "\uD83C\uDFA7 USB audio"
                        OutputKind.OTHER -> "\uD83C\uDFA7 " + device.label
                    }
                    views.setTextViewText(R.id.widget_device_chip, label)
                    views.setTextColor(
                        R.id.widget_device_chip,
                        if (isLight || isPixel) Color.parseColor("#12151B") else Color.WHITE
                    )
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

                if (settings.showVisualizer) {
                    val barColor = if (isLight || isPixel) Color.parseColor("#12151B") else Color.WHITE
                    val bars = WidgetRenderer.visualizerBitmap(
                        seedText = trackState.title + trackState.artist,
                        color = barColor,
                        pixelStyle = isPixel,
                        dimmed = !trackState.isPlaying
                    )
                    views.setImageViewBitmap(R.id.widget_visualizer, bars)
                    views.setViewVisibility(R.id.widget_visualizer, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_visualizer, View.GONE)
                }
            }

            // Pending Intents for controls
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
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id)
                }
            }
            SonoraCoverWidgetProvider.updateAll(context)
        }
    }
}
