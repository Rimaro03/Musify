package com.rimaro.musify.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track (
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val genre: String?,
    val artworkUrl: String?,
    var streamUrl: String?,
    val sourceUrl: String?
) : Parcelable