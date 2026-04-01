package com.rimaro.musify.data.extractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class Extractor @Inject constructor(
    private val newPipeExtractorImpl: NewPipeExtractor,
    private val innerTubeSearch: InnerTubeSearch,
    @Suppress("UNUSED_PARAMETER") newPipeInit: Boolean
) : TrackExtractor {

    override suspend fun extract(title: String, artist: String): ExtractorResult {
        return withContext(Dispatchers.IO) {
            val youtubeUrl = newPipeExtractorImpl.search(title, artist)
                //?: innerTubeSearch.search(title, artist)
                ?: return@withContext ExtractorResult.Failure("No results found for $title - $artist")

            Log.d("Extractor", "Found YouTube URL: $youtubeUrl")

            extractDirect(youtubeUrl)
        }
    }

    override suspend fun extractDirect(url: String): ExtractorResult {
        return withContext(Dispatchers.IO) {
            newPipeExtractorImpl.extractDirect(url)
                //?: innerTubeSearch.extractDirect(url)
                ?: ExtractorResult.Failure("Could not extract stream for $url")
        }
    }

}