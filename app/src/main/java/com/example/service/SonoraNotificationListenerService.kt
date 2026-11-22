package com.example.service

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.widget.SonoraAppWidgetProvider

class SonoraNotificationListenerService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())

    // Widgets cannot animate, so while a song plays we redraw them every few seconds
    // (this moves the progress bars on the widget).
    private val ticker = object : Runnable {
        override fun run() {
            if (MediaPlaybackManager.trackState.value.isPlaying) {
                SonoraAppWidgetProvider.updateAllWidgets(applicationContext)
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val component = ComponentName(this, SonoraNotificationListenerService::class.java)
        MediaPlaybackManager.init(this, component)
        val appContext = applicationContext
        MediaPlaybackManager.setOnStateChangedListener {
            SonoraAppWidgetProvider.updateAllWidgets(appContext)
        }
        SonoraAppWidgetProvider.updateAllWidgets(this)

        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, TICK_MS)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        handler.removeCallbacks(ticker)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        MediaPlaybackManager.refreshActiveSessions(this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        MediaPlaybackManager.refreshActiveSessions(this)
    }

    companion object {
        private const val TICK_MS = 5000L
    }
}
