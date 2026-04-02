package com.rimaro.musify.ui.search

import com.rimaro.musify.domain.dto.DeezerArtist
import com.rimaro.musify.domain.dto.DeezerGenre

sealed class TrendingUiState {
    object Idle : TrendingUiState()
    object Loading : TrendingUiState()
    data class Success(
        val artists: List<DeezerArtist>,
        val genres: List<DeezerGenre>
    ) : TrendingUiState()
    data class Error(val message: String) : TrendingUiState()

}