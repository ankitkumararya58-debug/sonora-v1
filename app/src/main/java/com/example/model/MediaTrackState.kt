package com.example.model

import android.graphics.Bitmap

data class MediaTrackState(
    val title: String = "No Song Playing",
    val artist: String = "Start playback in any media app",
    val album: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val albumArt: Bitmap? = null,
    val albumArtUri: String? = null,
    val packageName: String? = null,
    val appLabel: String? = null,
    val hasActiveSession: Boolean = false,
    val isDemoTrack: Boolean = false
)
