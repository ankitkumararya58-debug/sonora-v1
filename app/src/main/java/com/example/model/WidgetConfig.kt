package com.example.model

enum class WidgetPreset {
    COMPACT_PILL,
    EXPANDED_CARD
}

enum class WidgetTheme {
    DARK,
    LIGHT,
    TRANSPARENT
}

data class WidgetSettings(
    val preset: WidgetPreset = WidgetPreset.EXPANDED_CARD,
    val theme: WidgetTheme = WidgetTheme.DARK,
    val cornerRadiusDp: Int = 24,
    val opacityPercent: Int = 90,
    val showBlurBorder: Boolean = true,
    val customArtworkPath: String? = null,
    val useCustomArtwork: Boolean = false
)
