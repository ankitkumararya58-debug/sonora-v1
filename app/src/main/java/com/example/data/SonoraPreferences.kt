package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.WidgetPreset
import com.example.model.WidgetSettings
import com.example.model.WidgetTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SonoraPreferences(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sonora_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<WidgetSettings> = _settingsFlow.asStateFlow()

    fun loadSettings(): WidgetSettings {
        val presetStr = prefs.getString(KEY_PRESET, WidgetPreset.EXPANDED_CARD.name)
        val preset = try {
            WidgetPreset.valueOf(presetStr ?: WidgetPreset.EXPANDED_CARD.name)
        } catch (e: Exception) {
            WidgetPreset.EXPANDED_CARD
        }

        val themeStr = prefs.getString(KEY_THEME, WidgetTheme.DARK.name)
        val theme = try {
            WidgetTheme.valueOf(themeStr ?: WidgetTheme.DARK.name)
        } catch (e: Exception) {
            WidgetTheme.DARK
        }

        val cornerRadius = prefs.getInt(KEY_RADIUS, 24)
        val opacity = prefs.getInt(KEY_OPACITY, 90)
        val showBlurBorder = prefs.getBoolean(KEY_BLUR, true)
        val useCustomArtwork = prefs.getBoolean(KEY_USE_CUSTOM_ARTWORK, false)

        val customCoverFile = CustomArtworkManager.getCustomCoverFile(context)
        val customPath = if (customCoverFile.exists()) customCoverFile.absolutePath else null

        return WidgetSettings(
            preset = preset,
            theme = theme,
            cornerRadiusDp = cornerRadius,
            opacityPercent = opacity,
            showBlurBorder = showBlurBorder,
            customArtworkPath = customPath,
            useCustomArtwork = useCustomArtwork && customPath != null
        )
    }

    fun savePreset(preset: WidgetPreset) {
        prefs.edit().putString(KEY_PRESET, preset.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(preset = preset)
    }

    fun saveTheme(theme: WidgetTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(theme = theme)
    }

    fun saveCornerRadius(radiusDp: Int) {
        prefs.edit().putInt(KEY_RADIUS, radiusDp).apply()
        _settingsFlow.value = _settingsFlow.value.copy(cornerRadiusDp = radiusDp)
    }

    fun saveOpacity(opacityPercent: Int) {
        prefs.edit().putInt(KEY_OPACITY, opacityPercent).apply()
        _settingsFlow.value = _settingsFlow.value.copy(opacityPercent = opacityPercent)
    }

    fun saveShowBlurBorder(show: Boolean) {
        prefs.edit().putBoolean(KEY_BLUR, show).apply()
        _settingsFlow.value = _settingsFlow.value.copy(showBlurBorder = show)
    }

    fun setUseCustomArtwork(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_CUSTOM_ARTWORK, enabled).apply()
        val customCoverFile = CustomArtworkManager.getCustomCoverFile(context)
        val customPath = if (customCoverFile.exists()) customCoverFile.absolutePath else null
        _settingsFlow.value = _settingsFlow.value.copy(
            useCustomArtwork = enabled && customPath != null,
            customArtworkPath = customPath
        )
    }

    fun refreshCustomArtworkState() {
        val hasCustom = CustomArtworkManager.hasCustomCover(context)
        val customPath = if (hasCustom) CustomArtworkManager.getCustomCoverFile(context).absolutePath else null
        val enabled = prefs.getBoolean(KEY_USE_CUSTOM_ARTWORK, false) && hasCustom
        _settingsFlow.value = _settingsFlow.value.copy(
            customArtworkPath = customPath,
            useCustomArtwork = enabled
        )
    }

    companion object {
        private const val KEY_PRESET = "widget_preset"
        private const val KEY_THEME = "widget_theme"
        private const val KEY_RADIUS = "widget_radius"
        private const val KEY_OPACITY = "widget_opacity"
        private const val KEY_BLUR = "widget_blur"
        private const val KEY_USE_CUSTOM_ARTWORK = "widget_use_custom_artwork"
    }
}
