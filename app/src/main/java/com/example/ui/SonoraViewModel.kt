package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CustomArtworkManager
import com.example.data.SonoraPreferences
import com.example.model.MediaTrackState
import com.example.model.WidgetPreset
import com.example.model.WidgetSettings
import com.example.model.WidgetTheme
import com.example.service.MediaPlaybackManager
import com.example.widget.SonoraAppWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SonoraViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SonoraPreferences(application)

    val trackState: StateFlow<MediaTrackState> = MediaPlaybackManager.trackState

    val widgetSettings: StateFlow<WidgetSettings> = preferences.settingsFlow

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        MediaPlaybackManager.init(application)
        MediaPlaybackManager.setOnStateChangedListener {
            SonoraAppWidgetProvider.updateAllWidgets(getApplication())
        }
        checkPermission()
    }

    fun checkPermission() {
        val granted = MediaPlaybackManager.isNotificationListenerEnabled(getApplication())
        _isPermissionGranted.value = granted
        if (granted) {
            MediaPlaybackManager.refreshActiveSessions(getApplication())
        }
    }

    fun togglePlayPause() {
        MediaPlaybackManager.togglePlayPause()
        syncToWidgets()
    }

    fun skipToNext() {
        MediaPlaybackManager.skipToNext()
        syncToWidgets()
    }

    fun skipToPrevious() {
        MediaPlaybackManager.skipToPrevious()
        syncToWidgets()
    }

    fun enableDemoTrack() {
        MediaPlaybackManager.enableDemoTrack()
        syncToWidgets()
    }

    fun clearDemoTrack() {
        MediaPlaybackManager.clearDemoTrack(getApplication())
        syncToWidgets()
    }

    fun saveCustomArtwork(uri: Uri) {
        viewModelScope.launch {
            val success = CustomArtworkManager.saveCustomArtworkFromUri(getApplication(), uri)
            if (success) {
                preferences.refreshCustomArtworkState()
                preferences.setUseCustomArtwork(true)
                syncToWidgets()
                _toastMessage.value = "Custom cover artwork updated!"
            } else {
                _toastMessage.value = "Failed to load selected image"
            }
        }
    }

    fun removeCustomArtwork() {
        viewModelScope.launch {
            CustomArtworkManager.removeCustomArtwork(getApplication())
            preferences.setUseCustomArtwork(false)
            preferences.refreshCustomArtworkState()
            syncToWidgets()
            _toastMessage.value = "Restored original album artwork"
        }
    }

    fun setUseCustomArtwork(enabled: Boolean) {
        preferences.setUseCustomArtwork(enabled)
        syncToWidgets()
    }

    fun setPreset(preset: WidgetPreset) {
        preferences.savePreset(preset)
        syncToWidgets()
    }

    fun setTheme(theme: WidgetTheme) {
        preferences.saveTheme(theme)
        syncToWidgets()
    }

    fun setCornerRadius(radiusDp: Int) {
        preferences.saveCornerRadius(radiusDp)
        syncToWidgets()
    }

    fun setOpacity(opacityPercent: Int) {
        preferences.saveOpacity(opacityPercent)
        syncToWidgets()
    }

    fun setShowBlurBorder(show: Boolean) {
        preferences.saveShowBlurBorder(show)
        syncToWidgets()
    }

    fun syncToWidgets() {
        SonoraAppWidgetProvider.updateAllWidgets(getApplication())
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
