package com.rimaro.musify.ui.common.model

import com.rimaro.musify.domain.model.Track

data class TrackUiModel (
    val track: Track,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false
)