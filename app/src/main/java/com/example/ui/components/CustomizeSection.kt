package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CustomArtworkManager
import com.example.model.MediaTrackState
import com.example.model.WidgetPreset
import com.example.model.WidgetSettings
import com.example.model.WidgetTheme
import com.example.ui.SonoraViewModel
import com.example.ui.theme.SonoraAccentCyan
import com.example.ui.theme.SonoraAccentSilver
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraBorderHighlight
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceVariant
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

@Composable
fun CustomizeSection(
    viewModel: SonoraViewModel,
    trackState: MediaTrackState,
    widgetSettings: WidgetSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val customCoverFile = CustomArtworkManager.getCustomCoverFile(context)
    val hasCustomCover = customCoverFile.exists() && customCoverFile.length() > 0

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Widget Studio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SonoraTextPrimary
        )
        Text(
            text = "Live preview and home screen widget styling",
            style = MaterialTheme.typography.bodySmall,
            color = SonoraTextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Widget Live Preview Container (Simulating Android Home Screen backdrop)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, SonoraBorderHighlight, RoundedCornerShape(20.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            WidgetLivePreview(
                settings = widgetSettings,
                trackState = trackState,
                hasCustomCover = hasCustomCover,
                customCoverFile = customCoverFile,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.skipToNext() },
                onPrev = { viewModel.skipToPrevious() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preset Selector
        Text(
            text = "LAYOUT PRESET",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = SonoraTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PresetChip(
                label = "Expanded Card",
                selected = widgetSettings.preset == WidgetPreset.EXPANDED_CARD,
                onClick = { viewModel.setPreset(WidgetPreset.EXPANDED_CARD) },
                modifier = Modifier.weight(1f).testTag("preset_card")
            )
            PresetChip(
                label = "Compact Pill",
                selected = widgetSettings.preset == WidgetPreset.COMPACT_PILL,
                onClick = { viewModel.setPreset(WidgetPreset.COMPACT_PILL) },
                modifier = Modifier.weight(1f).testTag("preset_pill")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Theme Selector: Dark, Light, Transparent
        Text(
            text = "THEME",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = SonoraTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeChip(
                label = "Dark",
                selected = widgetSettings.theme == WidgetTheme.DARK,
                onClick = { viewModel.setTheme(WidgetTheme.DARK) },
                modifier = Modifier.weight(1f).testTag("theme_dark")
            )
            ThemeChip(
                label = "Light",
                selected = widgetSettings.theme == WidgetTheme.LIGHT,
                onClick = { viewModel.setTheme(WidgetTheme.LIGHT) },
                modifier = Modifier.weight(1f).testTag("theme_light")
            )
            ThemeChip(
                label = "Transparent",
                selected = widgetSettings.theme == WidgetTheme.TRANSPARENT,
                onClick = { viewModel.setTheme(WidgetTheme.TRANSPARENT) },
                modifier = Modifier.weight(1f).testTag("theme_transparent")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Corner Radius Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CORNER RADIUS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = SonoraTextMuted
            )
            Text(
                text = "${widgetSettings.cornerRadiusDp} dp",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = SonoraAccentCyan
            )
        }
        Slider(
            value = widgetSettings.cornerRadiusDp.toFloat(),
            onValueChange = { viewModel.setCornerRadius(it.toInt()) },
            valueRange = 12f..36f,
            steps = 23,
            colors = SliderDefaults.colors(
                thumbColor = SonoraAccentSilver,
                activeTrackColor = SonoraAccentCyan,
                inactiveTrackColor = SonoraSurfaceVariant
            ),
            modifier = Modifier.testTag("slider_corner_radius")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Background Opacity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BACKGROUND OPACITY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = SonoraTextMuted
            )
            Text(
                text = "${widgetSettings.opacityPercent}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = SonoraAccentCyan
            )
        }
        Slider(
            value = widgetSettings.opacityPercent.toFloat(),
            onValueChange = { viewModel.setOpacity(it.toInt()) },
            valueRange = 20f..100f,
            steps = 79,
            colors = SliderDefaults.colors(
                thumbColor = SonoraAccentSilver,
                activeTrackColor = SonoraAccentCyan,
                inactiveTrackColor = SonoraSurfaceVariant
            ),
            modifier = Modifier.testTag("slider_opacity")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Frosted Border Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Frosted Outline & Shadow",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SonoraTextPrimary
                )
                Text(
                    text = "Add subtle border highlight for translucent depth",
                    style = MaterialTheme.typography.labelSmall,
                    color = SonoraTextSecondary
                )
            }
            Switch(
                checked = widgetSettings.showBlurBorder,
                onCheckedChange = { viewModel.setShowBlurBorder(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SonoraBackground,
                    checkedTrackColor = SonoraAccentCyan,
                    uncheckedThumbColor = SonoraTextMuted,
                    uncheckedTrackColor = SonoraSurfaceVariant
                ),
                modifier = Modifier.testTag("switch_frosted_border")
            )
        }

        // Custom Artwork for Widget Toggle (if custom artwork exists)
        if (hasCustomCover) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Use Custom Cover on Widget",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = "Show chosen gallery cover on the widget",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTextSecondary
                    )
                }
                Switch(
                    checked = widgetSettings.useCustomArtwork,
                    onCheckedChange = { viewModel.setUseCustomArtwork(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SonoraBackground,
                        checkedTrackColor = SonoraAccentCyan,
                        uncheckedThumbColor = SonoraTextMuted,
                        uncheckedTrackColor = SonoraSurfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_custom_artwork")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Apply & Sync to Home Screen Widget Button
        Button(
            onClick = { viewModel.syncToWidgets() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_sync_widget"),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SonoraAccentSilver,
                contentColor = SonoraBackground
            )
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apply to Home Screen Widget",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WidgetLivePreview(
    settings: WidgetSettings,
    trackState: MediaTrackState,
    hasCustomCover: Boolean,
    customCoverFile: java.io.File,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val context = LocalContext.current
    val isPill = settings.preset == WidgetPreset.COMPACT_PILL
    val cornerRadius = if (isPill) 100.dp else settings.cornerRadiusDp.dp

    val baseColor = when (settings.theme) {
        WidgetTheme.DARK -> Color(0xFF12151B)
        WidgetTheme.LIGHT -> Color(0xFFFFFFFF)
        WidgetTheme.TRANSPARENT -> Color(0xFF000000)
    }

    val alpha = when (settings.theme) {
        WidgetTheme.TRANSPARENT -> (settings.opacityPercent / 100f * 0.45f)
        else -> (settings.opacityPercent / 100f)
    }

    val backgroundColor = baseColor.copy(alpha = alpha)

    val textColor = when (settings.theme) {
        WidgetTheme.LIGHT -> Color(0xFF12151B)
        else -> Color(0xFFFFFFFF)
    }

    val secondaryTextColor = when (settings.theme) {
        WidgetTheme.LIGHT -> Color(0xFF5B6472)
        else -> Color(0xFFA0A8B5)
    }

    val borderModifier = if (settings.showBlurBorder) {
        val borderColor = when (settings.theme) {
            WidgetTheme.LIGHT -> Color(0x26000000)
            WidgetTheme.TRANSPARENT -> Color(0x66FFFFFF)
            WidgetTheme.DARK -> Color(0x33FFFFFF)
        }
        Modifier.border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
    } else Modifier

    val shadowElevation = if (settings.showBlurBorder) 8.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadowElevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(borderModifier)
            .padding(if (isPill) 8.dp else 14.dp)
            .testTag("widget_live_preview_card")
    ) {
        if (isPill) {
            // Pill Preview Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular artwork
                ArtworkThumb(
                    trackState = trackState,
                    hasCustomCover = hasCustomCover,
                    customCoverFile = customCoverFile,
                    useCustom = settings.useCustomArtwork,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackState.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = trackState.artist,
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (trackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = textColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // Card Preview Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtworkThumb(
                    trackState = trackState,
                    hasCustomCover = hasCustomCover,
                    customCoverFile = customCoverFile,
                    useCustom = settings.useCustomArtwork,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackState.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = trackState.artist,
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPrev,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (trackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtworkThumb(
    trackState: MediaTrackState,
    hasCustomCover: Boolean,
    customCoverFile: java.io.File,
    useCustom: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.background(SonoraSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            useCustom && hasCustomCover -> {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(customCoverFile).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            trackState.albumArt != null -> {
                Image(
                    bitmap = trackState.albumArt.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            trackState.albumArtUri != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(trackState.albumArtUri).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SonoraTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) SonoraAccentSilver else SonoraSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) SonoraAccentSilver else SonoraBorder
        ),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) SonoraBackground else SonoraTextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) SonoraAccentSilver else SonoraSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) SonoraAccentSilver else SonoraBorder
        ),
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) SonoraBackground else SonoraTextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
    }
}
