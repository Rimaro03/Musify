package com.rimaro.musify.data.local.extractor

import com.rimaro.musify.data.local.extractor.model.ExtractorResult

interface TrackExtractor {
    suspend fun extract(title: String, artist: String): ExtractorResult
    suspend fun extractDirect(url: String): ExtractorResult
}