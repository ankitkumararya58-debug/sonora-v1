package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CustomArtworkManager
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
fun SettingsSection(
    viewModel: SonoraViewModel,
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SonoraTextPrimary
        )
        Text(
            text = "Permissions, artwork management, and widget guide",
            style = MaterialTheme.typography.bodySmall,
            color = SonoraTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 1: Notification & MediaSession Access
        SettingsCard(
            title = "Media Session Access",
            icon = Icons.Default.Security
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPermissionGranted) SonoraGreen else SonoraAccentCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPermissionGranted) "Access Granted" else "Access Required",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SonoraTextPrimary
                    )
                }

                Row {
                    IconButton(
                        onClick = { viewModel.checkPermission() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SonoraTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(100.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorderHighlight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraTextPrimary),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_settings_permission")
                    ) {
                        Text("Configure", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android requires Notification Listener permission to read playback metadata and dispatch play/pause/skip commands to media apps.",
                style = MaterialTheme.typography.bodySmall,
                color = SonoraTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: Custom Cover Management
        SettingsCard(
            title = "Custom Artwork",
            icon = Icons.Default.Image
        ) {
            if (hasCustomCover) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customCoverFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Custom Cover Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, SonoraBorderHighlight, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Artwork Active",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = "${customCoverFile.length() / 1024} KB • Saved locally",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonoraTextSecondary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.removeCustomArtwork() },
                        modifier = Modifier.testTag("btn_delete_custom_cover")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Custom Cover",
                            tint = SonoraTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonoraSurfaceVariant,
                        contentColor = SonoraTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose Another Image", fontSize = 12.sp)
                }
            } else {
                Text(
                    text = "No custom artwork is currently selected. The app and widget will display the active player's original album cover.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SonoraTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonoraAccentCyan,
                        contentColor = SonoraBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_settings_pick_artwork")
                ) {
                    Text("Select Custom Cover from Gallery", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: Home Screen Widget Guide
        SettingsCard(
            title = "Adding Widget to Home Screen",
            icon = Icons.Default.Widgets
        ) {
            WidgetGuideStep(
                number = "1",
                text = "Long-press an empty space on your Android home screen."
            )
            WidgetGuideStep(
                number = "2",
                text = "Select \"Widgets\" from the popup menu."
            )
            WidgetGuideStep(
                number = "3",
                text = "Scroll down to \"SONORA\" and drag the widget onto your home screen."
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: About SONORA
        SettingsCard(
            title = "About SONORA",
            icon = Icons.Default.Info
        ) {
            Text(
                text = "SONORA is a minimal music widget customization app built with Jetpack Compose. It connects directly to Android MediaSession APIs to control playback and display real metadata.",
                style = MaterialTheme.typography.bodySmall,
                color = SonoraTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Version 1.0 (Prototype)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SonoraTextMuted
                )
                Text(
                    text = "Zero Ads • Private • Local Only",
                    style = MaterialTheme.typography.labelSmall,
                    color = SonoraAccentCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SonoraSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SonoraAccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SonoraTextPrimary
                )
            }
            content()
        }
    }
}

@Composable
fun WidgetGuideStep(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = SonoraSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorderHighlight),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SonoraAccentCyan
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = SonoraTextSecondary
        )
    }
}
