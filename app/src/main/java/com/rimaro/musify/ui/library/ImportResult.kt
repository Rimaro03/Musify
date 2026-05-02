package com.rimaro.musify.ui.library

sealed class ImportResult {
    data class Progress(val processed: Int, val total: Int, val failed: Int) : ImportResult()
    data class Success(val imported: Int, val skipped: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}