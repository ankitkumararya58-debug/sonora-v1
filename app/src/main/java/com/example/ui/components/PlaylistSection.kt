package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.WidgetSettings
import com.example.ui.SonoraViewModel
import com.example.ui.theme.SonoraAccentCyan
import com.example.ui.theme.SonoraBackground
import com.example.ui.theme.SonoraBorder
import com.example.ui.theme.SonoraBorderHighlight
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceVariant
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

private val LIST_PARAM = Regex("[?&]list=([A-Za-z0-9_-]+)")
private val RAW_ID = Regex("^(PL|OL|UU|LL|FL|RD)[A-Za-z0-9_-]{10,}$")

/** Accepts a full YouTube / YouTube Music playlist link, or a bare playlist id. */
fun extractPlaylistId(input: String): String? {
    val text = input.trim()
    LIST_PARAM.find(text)?.let { return it.groupValues[1] }
    if (RAW_ID.matches(text)) return text
    return null
}

private fun openLink(context: android.content.Context, url: String, packageName: String? = null) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (packageName != null) setPackage(packageName)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        if (packageName != null) {
            // app not installed: open in browser instead
            openLink(context, url, null)
        }
    }
}

@Composable
fun PlaylistSection(
    viewModel: SonoraViewModel,
    widgetSettings: WidgetSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedUrl = widgetSettings.playlistUrl
    val savedId = extractPlaylistId(savedUrl)

    var input by rememberSaveable { mutableStateOf(savedUrl) }
    var editing by rememberSaveable { mutableStateOf(savedId == null) }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = SonoraAccentCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "My Playlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SonoraTextPrimary
            )
        }
        Text(
            text = "Your own YouTube playlist, one tap away",
            style = MaterialTheme.typography.bodySmall,
            color = SonoraTextSecondary
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (editing || savedId == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SonoraSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paste your playlist link",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SonoraTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Example: https://www.youtube.com/playlist?list=PLxxxxxxxx (YouTube or YouTube Music links both work)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            error = null
                        },
                        singleLine = true,
                        placeholder = { Text("youtube.com/playlist?list=...", fontSize = 12.sp) },
                        isError = error != null,
                        supportingText = if (error != null) { { Text(error ?: "") } } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SonoraTextPrimary,
                            unfocusedTextColor = SonoraTextPrimary,
                            focusedBorderColor = SonoraAccentCyan,
                            unfocusedBorderColor = SonoraBorderHighlight,
                            cursorColor = SonoraAccentCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val id = extractPlaylistId(input)
                            if (id == null) {
                                error = "That is not a YouTube playlist link (it must contain list=...)"
                            } else {
                                viewModel.setPlaylistUrl("https://www.youtube.com/playlist?list=$id")
                                editing = false
                                error = null
                            }
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonoraAccentCyan,
                            contentColor = SonoraBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_playlist")
                    ) {
                        Text("Save playlist", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            val id: String = savedId ?: ""
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SonoraSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saved playlist",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SonoraTextPrimary
                            )
                            Text(
                                text = id,
                                style = MaterialTheme.typography.labelSmall,
                                color = SonoraTextMuted,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                input = savedUrl
                                editing = true
                            },
                            modifier = Modifier.testTag("btn_edit_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Change playlist",
                                tint = SonoraTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            openLink(
                                context,
                                "https://music.youtube.com/playlist?list=$id",
                                "com.google.android.apps.youtube.music"
                            )
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonoraAccentCyan,
                            contentColor = SonoraBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_play_ytmusic")
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play in YouTube Music", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            openLink(
                                context,
                                "https://www.youtube.com/playlist?list=$id",
                                "com.google.android.youtube"
                            )
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraTextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorderHighlight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_youtube")
                    ) {
                        Icon(Icons.Filled.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in YouTube", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showPreview = !showPreview },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraTextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SonoraBorderHighlight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_preview_playlist")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showPreview) "Hide in-app player" else "Preview inside Sonora", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tip: play it in YouTube Music or YouTube and your Sonora widgets will show the cover and control it (play / pause / next). The in-app preview is only a mini player and widgets can't control it.",
                style = MaterialTheme.typography.labelSmall,
                color = SonoraTextSecondary
            )

            if (showPreview) {
                Spacer(modifier = Modifier.height(12.dp))
                PlaylistWebPlayer(playlistId = id)
                Text(
                    text = "If a video says it can't be played here, the owner blocked embedding. Use the buttons above.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SonoraTextMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PlaylistWebPlayer(playlistId: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.BLACK)
                val html = "<html><body style=\"margin:0;background:#000\">" +
                    "<iframe style=\"position:absolute;top:0;left:0;width:100%;height:100%\" " +
                    "src=\"https://www.youtube.com/embed/videoseries?list=" + playlistId + "&playsinline=1\" " +
                    "frameborder=\"0\" allow=\"autoplay; encrypted-media; picture-in-picture\" allowfullscreen>" +
                    "</iframe></body></html>"
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .testTag("playlist_webview")
    )
}
