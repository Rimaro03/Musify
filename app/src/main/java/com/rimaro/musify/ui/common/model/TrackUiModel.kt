package com.rimaro.musify.ui.common.model

import android.os.Parcelable
import com.rimaro.musify.domain.model.Track
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackUiModel (
    val track: Track,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false
) : Parcelable