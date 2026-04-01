package com.rimaro.musify.data.extractor

sealed class ExtractorResult {
    data class Success(
        val streamUrl: String,
        val expiresAt: Long,
        val sourceUrl: String
    ) : ExtractorResult()

    data class Failure(val reason: String) : ExtractorResult()
}