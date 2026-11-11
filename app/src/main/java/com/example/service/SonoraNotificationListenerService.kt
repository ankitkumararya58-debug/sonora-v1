package com.example.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.widget.SonoraAppWidgetProvider

class SonoraNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        val component = ComponentName(this, SonoraNotificationListenerService::class.java)
        MediaPlaybackManager.init(this, component)
        SonoraAppWidgetProvider.updateAllWidgets(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        MediaPlaybackManager.refreshActiveSessions(this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        MediaPlaybackManager.refreshActiveSessions(this)
    }
}
