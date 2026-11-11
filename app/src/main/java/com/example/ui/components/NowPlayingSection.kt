package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CustomArtworkManager
import com.example.model.MediaTrackState
import com.example.model.WidgetSettings
import com.example.ui.SonoraViewModel
import com.example.ui.theme.SonoraAccentCyan
import com.example.ui.theme.SonoraAccentSilver
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraBorderHighlight
import com.example.ui.theme.SonoraGreen
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceVariant
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

@Composable
fun NowPlayingSection(
    viewModel: SonoraViewModel,
    trackState: MediaTrackState,
    widgetSettings: WidgetSettings,
    isPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveCustomArtwork(uri)
        }
    }

    val customCoverFile = CustomArtworkManager.getCustomCoverFile(context)
    val hasCustomCover = customCoverFile.exists() && customCoverFile.length() > 0

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Brand & Playback Source Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = SonoraAccentCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SONORA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = SonoraAccentSilver
                )
            }

            // Player Source badge
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = SonoraSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (trackState.isPlaying) SonoraGreen else SonoraTextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            trackState.isDemoTrack -> "Demo Player"
                            trackState.appLabel != null -> trackState.appLabel
                            trackState.hasActiveSession -> "Active Session"
                            else -> "Standby"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTextSecondary
                    )
                }
            }
        }

        // Notification Listener Permission Banner if not granted
        AnimatedVisibility(visible = !isPermissionGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("permission_banner_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SonoraSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorderHighlight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notification Access Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SonoraAccentCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To detect active playback from Spotify, YouTube Music, and other media players, please grant Notification Access in Android Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("btn_grant_permission"),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SonoraAccentCyan,
                                contentColor = SonoraBackground
                            )
                        ) {
                            Text("Open Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Artwork Display Card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(widgetSettings.cornerRadiusDp.dp))
                .clip(RoundedCornerShape(widgetSettings.cornerRadiusDp.dp))
                .border(
                    width = 1.dp,
                    color = SonoraBorderHighlight,
                    shape = RoundedCornerShape(widgetSettings.cornerRadiusDp.dp)
                )
                .background(SonoraSurface)
                .testTag("artwork_card"),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Custom Artwork override
                widgetSettings.useCustomArtwork && hasCustomCover -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customCoverFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Custom Cover Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
                // Bitmap from active MediaSession
                trackState.albumArt != null -> {
                    Image(
                        bitmap = trackState.albumArt.asImageBitmap(),
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
                // Web/Uri Album art
                trackState.albumArtUri != null -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(trackState.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
                // Sleek minimalist acoustic waveform fallback
                else -> {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(SonoraSurfaceVariant, SonoraSurface)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = SonoraBorderHighlight,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SONORA",
                                style = MaterialTheme.typography.labelMedium,
                                color = SonoraTextMuted,
                                letterSpacing = 3.sp
                            )
                        }
                    }
                }
            }

            // Custom cover indicator badge in top right
            if (widgetSettings.useCustomArtwork && hasCustomCover) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = SonoraBackground.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SonoraAccentCyan),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Custom Cover",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraAccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Song Title
        Text(
            text = trackState.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SonoraTextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("song_title")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist Name
        Text(
            text = trackState.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = SonoraTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("artist_name")
        )

        // Progress bar if duration available
        if (trackState.durationMs > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            val progress = (trackState.positionMs.toFloat() / trackState.durationMs.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(4.dp)
                    .clip(CircleShape),
                color = SonoraAccentCyan,
                trackColor = SonoraSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Transport Playback Controls (Prev, Play/Pause, Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            IconButton(
                onClick = { viewModel.skipToPrevious() },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("btn_previous"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = SonoraSurfaceVariant,
                    contentColor = SonoraTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Play / Pause Button
            FilledIconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(68.dp)
                    .shadow(12.dp, CircleShape)
                    .testTag("btn_play_pause"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SonoraAccentSilver,
                    contentColor = SonoraBackground
                )
            ) {
                Icon(
                    imageVector = if (trackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (trackState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Next Button
            IconButton(
                onClick = { viewModel.skipToNext() },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("btn_next"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = SonoraSurfaceVariant,
                    contentColor = SonoraTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Custom Artwork action button
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.testTag("btn_change_cover"),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorderHighlight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SonoraTextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SonoraAccentCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Cover", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Reset cover button if custom cover is active
            if (hasCustomCover) {
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    onClick = { viewModel.removeCustomArtwork() },
                    modifier = Modifier.testTag("btn_reset_cover")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = SonoraTextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", color = SonoraTextSecondary, fontSize = 13.sp)
                }
            }
        }

        // Demo Track toggle (convenient testing button)
        Spacer(modifier = Modifier.height(16.dp))
        if (!trackState.hasActiveSession || trackState.isDemoTrack) {
            TextButton(
                onClick = {
                    if (trackState.isDemoTrack) {
                        viewModel.clearDemoTrack()
                    } else {
                        viewModel.enableDemoTrack()
                    }
                },
                modifier = Modifier.testTag("btn_toggle_demo")
            ) {
                Text(
                    text = if (trackState.isDemoTrack) "Exit Demo Mode" else "Try Sonora Demo Track",
                    color = SonoraAccentCyan,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
