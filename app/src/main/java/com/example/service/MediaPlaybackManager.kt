package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.model.MediaTrackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaPlaybackManager {

    private val _trackState = MutableStateFlow(MediaTrackState())
    val trackState: StateFlow<MediaTrackState> = _trackState.asStateFlow()

    private var activeController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var sessionsChangedListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var notificationListenerComponent: ComponentName? = null
    private var isInitialized = false

    private var onStateChangedListener: (() -> Unit)? = null

    fun setOnStateChangedListener(listener: () -> Unit) {
        onStateChangedListener = listener
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController(activeController)
        }

        override fun onSessionDestroyed() {
            activeController = null
            _trackState.value = _trackState.value.copy(
                hasActiveSession = false,
                isPlaying = false
            )
            notifyStateChanged()
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun init(context: Context, serviceComponent: ComponentName? = null) {
        val appContext = context.applicationContext
        if (serviceComponent != null) {
            notificationListenerComponent = serviceComponent
        } else if (notificationListenerComponent == null) {
            notificationListenerComponent = ComponentName(appContext, SonoraNotificationListenerService::class.java)
        }

        if (mediaSessionManager == null) {
            mediaSessionManager = appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        }

        refreshActiveSessions(appContext)
        isInitialized = true
    }

    fun refreshActiveSessions(context: Context) {
        val msm = mediaSessionManager ?: return
        val component = notificationListenerComponent ?: return

        if (!isNotificationListenerEnabled(context)) {
            if (!_trackState.value.isDemoTrack) {
                _trackState.value = MediaTrackState(
                    title = "Media Access Needed",
                    artist = "Enable Notification Access in Settings",
                    hasActiveSession = false
                )
            }
            return
        }

        try {
            val controllers = msm.getActiveSessions(component)
            if (controllers.isNotEmpty()) {
                val newController = controllers.firstOrNull {
                    it.playbackState?.state == PlaybackState.STATE_PLAYING
                } ?: controllers.first()

                attachController(newController, context)
            } else {
                if (!_trackState.value.isDemoTrack) {
                    _trackState.value = MediaTrackState(
                        title = "No Music Playing",
                        artist = "Play a song in Spotify or any media app",
                        hasActiveSession = false
                    )
                    notifyStateChanged()
                }
            }

            if (sessionsChangedListener == null) {
                sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { newControllers ->
                    if (!newControllers.isNullOrEmpty()) {
                        val active = newControllers.firstOrNull {
                            it.playbackState?.state == PlaybackState.STATE_PLAYING
                        } ?: newControllers.first()
                        attachController(active, context)
                    } else {
                        detachCurrentController()
                        if (!_trackState.value.isDemoTrack) {
                            _trackState.value = MediaTrackState(
                                title = "No Music Playing",
                                artist = "Play a song in Spotify or any media app",
                                hasActiveSession = false
                            )
                            notifyStateChanged()
                        }
                    }
                }
                msm.addOnActiveSessionsChangedListener(sessionsChangedListener!!, component)
            }
        } catch (e: SecurityException) {
            // Permission not granted yet
            if (!_trackState.value.isDemoTrack) {
                _trackState.value = MediaTrackState(
                    title = "Media Access Required",
                    artist = "Tap to grant permission in Settings",
                    hasActiveSession = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachController(controller: MediaController, context: Context) {
        if (activeController?.sessionToken == controller.sessionToken) {
            updateFromController(controller, context)
            return
        }

        detachCurrentController()
        activeController = controller
        controller.registerCallback(controllerCallback)
        updateFromController(controller, context)
    }

    private fun detachCurrentController() {
        try {
            activeController?.unregisterCallback(controllerCallback)
        } catch (e: Exception) {
            // Ignored
        }
        activeController = null
    }

    private fun updateFromController(controller: MediaController?, context: Context? = null) {
        if (controller == null) return

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Title"

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: "Unknown Artist"

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        val artworkBitmap: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val artUri = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val position = playbackState?.position ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val pkgName = controller.packageName
        var appLabel: String? = null
        if (context != null && pkgName != null) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                appLabel = pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                appLabel = pkgName
            }
        }

        _trackState.value = MediaTrackState(
            title = title,
            artist = artist,
            album = album,
            isPlaying = isPlaying,
            durationMs = duration,
            positionMs = position,
            albumArt = artworkBitmap,
            albumArtUri = artUri,
            packageName = pkgName,
            appLabel = appLabel,
            hasActiveSession = true,
            isDemoTrack = false
        )

        notifyStateChanged()
    }

    private fun notifyStateChanged() {
        onStateChangedListener?.invoke()
    }

    fun play() {
        if (_trackState.value.isDemoTrack) {
            _trackState.value = _trackState.value.copy(isPlaying = true)
            notifyStateChanged()
            return
        }
        activeController?.transportControls?.play()
    }

    fun pause() {
        if (_trackState.value.isDemoTrack) {
            _trackState.value = _trackState.value.copy(isPlaying = false)
            notifyStateChanged()
            return
        }
        activeController?.transportControls?.pause()
    }

    fun togglePlayPause() {
        if (_trackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun skipToNext() {
        if (_trackState.value.isDemoTrack) {
            cycleDemoTrack(forward = true)
            return
        }
        activeController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        if (_trackState.value.isDemoTrack) {
            cycleDemoTrack(forward = false)
            return
        }
        activeController?.transportControls?.skipToPrevious()
    }

    // Demo Track support for immediate testing and simulation in headless/emulator test environments
    private val demoTracks = listOf(
        Pair("Midnight Waves", "Acoustic Aura • Sonora Sessions"),
        Pair("Velvet Horizon", "Neon Nocturne • Chillhop"),
        Pair("Obsidian Pulse", "Echo Mirage • Ambient Soundscape")
    )
    private var demoIndex = 0

    fun enableDemoTrack() {
        val current = demoTracks[demoIndex % demoTracks.size]
        _trackState.value = MediaTrackState(
            title = current.first,
            artist = current.second,
            album = "Sonora Sessions",
            isPlaying = true,
            durationMs = 214000L,
            positionMs = 65000L,
            hasActiveSession = true,
            isDemoTrack = true,
            appLabel = "Sonora Preview"
        )
        notifyStateChanged()
    }

    private fun cycleDemoTrack(forward: Boolean) {
        demoIndex = if (forward) demoIndex + 1 else if (demoIndex > 0) demoIndex - 1 else demoTracks.size - 1
        val current = demoTracks[demoIndex % demoTracks.size]
        _trackState.value = _trackState.value.copy(
            title = current.first,
            artist = current.second,
            isPlaying = true,
            positionMs = 12000L
        )
        notifyStateChanged()
    }

    fun clearDemoTrack(context: Context) {
        _trackState.value = MediaTrackState()
        refreshActiveSessions(context)
    }
}
