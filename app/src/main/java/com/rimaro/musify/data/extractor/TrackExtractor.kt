package com.rimaro.musify.data.extractor

interface TrackExtractor {
    suspend fun extract(title: String, artist: String): ExtractorResult
    suspend fun extractDirect(url: String): ExtractorResult
}