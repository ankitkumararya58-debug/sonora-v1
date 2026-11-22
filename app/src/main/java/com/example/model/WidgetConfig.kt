package com.example.model

enum class WidgetPreset {
    COMPACT_PILL,
    EXPANDED_CARD
}

enum class WidgetTheme {
    DARK,
    LIGHT,
    TRANSPARENT,
    PIXEL,
    GLASS
}

/** Shape used by the standalone "Cover" widget. */
enum class CoverShape {
    ROUNDED,
    CIRCLE,
    SQUIRCLE,
    PIXEL
}

data class WidgetSettings(
    val preset: WidgetPreset = WidgetPreset.EXPANDED_CARD,
    val theme: WidgetTheme = WidgetTheme.DARK,
    val cornerRadiusDp: Int = 24,
    val opacityPercent: Int = 90,
    val showBlurBorder: Boolean = true,
    val customArtworkPath: String? = null,
    val useCustomArtwork: Boolean = false,
    // --- v2 additions ---
    val coverShape: CoverShape = CoverShape.ROUNDED,
    val coverSizePercent: Int = 100,
    val showVisualizer: Boolean = true,
    val showDeviceChip: Boolean = true,
    val themedIcons: Boolean = true,
    val liveVisualizer: Boolean = false,
    val playlistUrl: String = ""
)
