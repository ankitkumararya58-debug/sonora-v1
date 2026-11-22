package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomArtworkManager
import com.example.model.CoverShape
import com.example.model.MediaTrackState
import com.example.model.WidgetSettings
import com.example.model.WidgetTheme
import com.example.service.OutputDevice
import com.example.service.OutputKind
import com.example.service.VisualizerEngine
import com.example.ui.SonoraViewModel
import com.example.ui.theme.SonoraAccentCyan
import com.example.ui.theme.SonoraAccentSilver
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraSurfaceVariant
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.widget.WidgetRenderer

/** Second row of theme chips (Pixel + iOS Glass), shown right under the old Dark / Light / Transparent row. */
@Composable
fun MoreThemeRow(settings: WidgetSettings, viewModel: SonoraViewModel) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeChip(
            label = "Pixel 8-bit",
            selected = settings.theme == WidgetTheme.PIXEL,
            onClick = { viewModel.setTheme(WidgetTheme.PIXEL) },
            modifier = Modifier.weight(1f).testTag("theme_pixel")
        )
        ThemeChip(
            label = "iOS Glass",
            selected = settings.theme == WidgetTheme.GLASS,
            onClick = { viewModel.setTheme(WidgetTheme.GLASS) },
            modifier = Modifier.weight(1f).testTag("theme_glass")
        )
    }
}

/** Cover widget shape/size, visualiser, device badge and themed icon controls. */
@Composable
fun StudioExtras(
    viewModel: SonoraViewModel,
    settings: WidgetSettings,
    trackState: MediaTrackState
) {
    val context = LocalContext.current
    val device by viewModel.outputDevice.collectAsState()

    val coverImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.saveCoverWidgetArtwork(uri)
    }
    val hasCoverWidgetImage = CustomArtworkManager.hasCoverWidgetCover(context)

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setLiveVisualizer(granted)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        ExtrasLabel("COVER WIDGET")
        Text(
            text = "Long-press your home screen, add the \"Sonora Cover\" widget, then style it here. Resize the widget on the home screen for a bigger cover.",
            style = MaterialTheme.typography.bodySmall,
            color = SonoraTextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val preview = remember(
            trackState.albumArt,
            settings.coverShape,
            settings.coverSizePercent,
            settings.cornerRadiusDp,
            settings.useCoverWidgetArtwork
        ) {
            val art = if (settings.useCoverWidgetArtwork) {
                CustomArtworkManager.loadCoverWidgetBitmap(context) ?: trackState.albumArt
            } else {
                trackState.albumArt
            }
            WidgetRenderer.shapedCover(
                context = context,
                art = art,
                shape = settings.coverShape,
                widthPx = 480,
                heightPx = 320,
                sizePercent = settings.coverSizePercent,
                cornerPercent = settings.cornerRadiusDp
            ).asImageBitmap()
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                bitmap = preview,
                contentDescription = "Cover widget preview",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(480f / 320f)
                    .testTag("cover_widget_preview")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeChip(
                label = "Rounded",
                selected = settings.coverShape == CoverShape.ROUNDED,
                onClick = { viewModel.setCoverShape(CoverShape.ROUNDED) },
                modifier = Modifier.weight(1f).testTag("shape_rounded")
            )
            ThemeChip(
                label = "Circle",
                selected = settings.coverShape == CoverShape.CIRCLE,
                onClick = { viewModel.setCoverShape(CoverShape.CIRCLE) },
                modifier = Modifier.weight(1f).testTag("shape_circle")
            )
            ThemeChip(
                label = "Squircle",
                selected = settings.coverShape == CoverShape.SQUIRCLE,
                onClick = { viewModel.setCoverShape(CoverShape.SQUIRCLE) },
                modifier = Modifier.weight(1f).testTag("shape_squircle")
            )
            ThemeChip(
                label = "Pixel",
                selected = settings.coverShape == CoverShape.PIXEL,
                onClick = { viewModel.setCoverShape(CoverShape.PIXEL) },
                modifier = Modifier.weight(1f).testTag("shape_pixel")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExtrasLabel("COVER SIZE", bottomPadding = 0)
            Text(
                text = "${settings.coverSizePercent}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = SonoraAccentCyan
            )
        }
        Slider(
            value = settings.coverSizePercent.toFloat(),
            onValueChange = { viewModel.setCoverSize(it.toInt()) },
            valueRange = 40f..100f,
            colors = SliderDefaults.colors(
                thumbColor = SonoraAccentSilver,
                activeTrackColor = SonoraAccentCyan,
                inactiveTrackColor = SonoraSurfaceVariant
            ),
            modifier = Modifier.testTag("slider_cover_size")
        )
        Text(
            text = "\"Rounded\" fills the widget as a rounded rectangle and uses the Corner Radius slider above.",
            style = MaterialTheme.typography.labelSmall,
            color = SonoraTextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))
        ExtrasLabel("COVER WIDGET IMAGE")
        Text(
            text = "Give the Cover widget its own picture. It is separate from the main widget's custom cover.",
            style = MaterialTheme.typography.bodySmall,
            color = SonoraTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    coverImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraTextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder),
                modifier = Modifier.weight(1f).testTag("btn_pick_cover_widget_image")
            ) {
                Text(if (hasCoverWidgetImage) "Change image" else "Choose image", fontSize = 13.sp)
            }
            if (hasCoverWidgetImage) {
                OutlinedButton(
                    onClick = { viewModel.removeCoverWidgetArtwork() },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder),
                    modifier = Modifier.weight(1f).testTag("btn_remove_cover_widget_image")
                ) {
                    Text("Use live cover", fontSize = 13.sp)
                }
            }
        }
        if (hasCoverWidgetImage) {
            ToggleRow(
                title = "Use my image on Cover widget",
                subtitle = "Off = the Cover widget follows the playing song",
                checked = settings.useCoverWidgetArtwork,
                onChange = { viewModel.setUseCoverWidgetArtwork(it) },
                tag = "switch_cover_widget_image"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        ExtrasLabel("EXTRAS")

        ToggleRow(
            title = "Audio visualiser",
            subtitle = "Short bars + dots on the widget (grow with the song) and live bars on Home",
            checked = settings.showVisualizer,
            onChange = { viewModel.setShowVisualizer(it) },
            tag = "switch_visualizer"
        )
        ToggleRow(
            title = "React to real audio (optional)",
            subtitle = "Needs microphone permission. Home screen bars follow the actual sound; otherwise they animate smoothly",
            checked = settings.liveVisualizer,
            onChange = { enabled ->
                if (enabled && !VisualizerEngine.hasPermission(context)) {
                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.setLiveVisualizer(enabled)
                }
            },
            tag = "switch_live_visualizer"
        )
        ToggleRow(
            title = "Playing-on badge",
            subtitle = "Shows Speaker / Earphones / Bluetooth name (now: ${device.label})",
            checked = settings.showDeviceChip,
            onChange = { viewModel.setShowDeviceChip(it) },
            tag = "switch_device_chip"
        )
        ToggleRow(
            title = "Themed control icons",
            subtitle = "Pixel theme gets blocky 8-bit buttons, Glass gets bubble buttons",
            checked = settings.themedIcons,
            onChange = { viewModel.setThemedIcons(it) },
            tag = "switch_themed_icons"
        )
        Text(
            text = "Home screen widgets can't animate, so the widget redraws every few seconds while music plays. The Default theme shows a thin progress line instead.",
            style = MaterialTheme.typography.labelSmall,
            color = SonoraTextMuted
        )
    }
}

@Composable
private fun ExtrasLabel(text: String, bottomPadding: Int = 8) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SonoraTextMuted,
        modifier = Modifier.padding(bottom = bottomPadding.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SonoraTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SonoraTextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SonoraBackground,
                checkedTrackColor = SonoraAccentCyan,
                uncheckedThumbColor = SonoraTextMuted,
                uncheckedTrackColor = SonoraSurfaceVariant
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

/** Device badge + visualiser shown on the Home tab under the cover. */
@Composable
fun HomeLiveExtras(
    trackState: MediaTrackState,
    settings: WidgetSettings,
    device: OutputDevice
) {
    if (!settings.showVisualizer && !settings.showDeviceChip) return

    Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        if (settings.showDeviceChip) {
            DeviceChip(device)
        }
        if (settings.showVisualizer) {
            Spacer(modifier = Modifier.height(12.dp))
            AudioVisualizerBars(
                isPlaying = trackState.isPlaying,
                seed = trackState.title + trackState.artist,
                color = SonoraAccentCyan,
                pixelStyle = settings.theme == WidgetTheme.PIXEL,
                liveEnabled = settings.liveVisualizer,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(28.dp)
                    .testTag("home_visualizer")
            )
        }
    }
}

@Composable
fun DeviceChip(device: OutputDevice) {
    val icon = when (device.kind) {
        OutputKind.PHONE_SPEAKER -> Icons.Filled.Speaker
        OutputKind.BLUETOOTH -> Icons.Filled.BluetoothAudio
        OutputKind.USB -> Icons.Filled.Usb
        else -> Icons.Filled.Headphones
    }
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = SonoraSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder),
        modifier = Modifier.testTag("device_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SonoraAccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Playing on " + device.label,
                style = MaterialTheme.typography.labelMedium,
                color = SonoraTextPrimary
            )
        }
    }
}
