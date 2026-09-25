package com.rimaro.musify.data.local.extractor

import com.rimaro.musify.data.local.extractor.model.ExtractorResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class Extractor @Inject constructor(
    private val newPipeExtractorImpl: NewPipeExtractor,
    @Suppress("UNUSED_PARAMETER") newPipeInit: Boolean
) : TrackExtractor {

    /**
     * Extract a track audio url from its title and artist
     * @param title the title of the track
     * @param artist the name of the artist
     * @return object containing streamUrl, expiration and sourceUrl for the provided track url
     */
    override suspend fun extract(title: String, artist: String): ExtractorResult {
        return withContext(Dispatchers.IO) {
            val youtubeUrl = newPipeExtractorImpl.search(title, artist)
                ?: return@withContext ExtractorResult.Failure("No results found for $title - $artist")

            extractDirect(youtubeUrl)
        }
    }

    /**
     * Extract a track audio url from its YouTube source url
     * @param url the url of the track
     * @return object containing streamUrl, expiration and sourceUrl for the provided track url
     */
    override suspend fun extractDirect(url: String): ExtractorResult {
        return withContext(Dispatchers.IO) {
            newPipeExtractorImpl.extractDirect(url)
                ?: ExtractorResult.Failure("Could not extract stream for $url")
        }
    }

}