package com.rimaro.musify.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackUiModel (
    val track: Track,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false
) : Parcelable