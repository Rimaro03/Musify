package com.rimaro.musify.ui.search

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val searchResultLis: List<SearchResultItem>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}