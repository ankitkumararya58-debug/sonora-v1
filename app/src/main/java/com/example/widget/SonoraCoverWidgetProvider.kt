package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.CustomArtworkManager
import com.example.data.SonoraPreferences
import com.example.service.MediaPlaybackManager

/**
 * A second widget that shows ONLY the cover, with a custom shape and size.
 * It has its OWN custom image (separate from the main widget's custom cover).
 */
class SonoraCoverWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
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

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            try {
                val settings = SonoraPreferences(context).loadSettings()
                val track = MediaPlaybackManager.trackState.value
                val m = WidgetRenderer.metricsFor(context, appWidgetManager, widgetId, 110)

                val customArt = if (settings.useCoverWidgetArtwork) {
                    CustomArtworkManager.loadCoverWidgetBitmap(context)
                } else null
                val art = customArt ?: track.albumArt

                val bitmap = WidgetRenderer.shapedCover(
                    context = context,
                    art = art,
                    shape = settings.coverShape,
                    widthPx = m.width,
                    heightPx = m.height,
                    sizePercent = settings.coverSizePercent,
                    cornerPercent = settings.cornerRadiusDp
                )

                val views = RemoteViews(context.packageName, R.layout.widget_sonora_cover)
                views.setImageViewBitmap(R.id.widget_cover_art, bitmap)

                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pending = PendingIntent.getActivity(
                    context,
                    1000 + widgetId,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_cover_root, pending)

                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (t: Throwable) {
                // never crash the process because of a widget
                t.printStackTrace()
            }
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SonoraCoverWidgetProvider::class.java))
            for (id in ids) updateWidget(context, manager, id)
        }
    }
}
