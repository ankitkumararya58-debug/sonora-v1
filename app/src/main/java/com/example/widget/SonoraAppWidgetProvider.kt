package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.CustomArtworkManager
import com.example.data.SonoraPreferences
import com.example.model.WidgetPreset
import com.example.model.WidgetTheme
import com.example.service.MediaPlaybackManager

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

            // Background selection
            val backgroundRes = when (settings.theme) {
                WidgetTheme.DARK -> if (isCard) R.drawable.widget_card_background_dark else R.drawable.widget_pill_background_dark
                WidgetTheme.LIGHT -> if (isCard) R.drawable.widget_card_background_light else R.drawable.widget_pill_background_light
                WidgetTheme.TRANSPARENT -> if (isCard) R.drawable.widget_card_background_trans else R.drawable.widget_pill_background_trans
            }
            views.setInt(R.id.widget_root, "setBackgroundResource", backgroundRes)

            // Text colors
            val isLight = settings.theme == WidgetTheme.LIGHT
            val titleColor = if (isLight) Color.parseColor("#12151B") else Color.parseColor("#FFFFFF")
            val artistColor = if (isLight) Color.parseColor("#5B6472") else Color.parseColor("#A0A8B5")

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

            // Play/Pause icon
            val playPauseRes = if (trackState.isPlaying) {
                R.drawable.ic_widget_pause
            } else {
                R.drawable.ic_widget_play
            }
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseRes)

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
        }
    }
}
