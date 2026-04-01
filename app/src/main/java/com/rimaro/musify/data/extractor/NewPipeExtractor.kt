package com.rimaro.musify.data.extractor

import android.util.Log
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import javax.inject.Inject

class NewPipeExtractor @Inject constructor(
    @Suppress("UNUSED_PARAMETER") newPipeInit: Boolean
) {

    private val youtubeService = ServiceList.YouTube

    fun search(title: String, artist: String): String? {
        return try {
            val queryHandler = youtubeService.searchQHFactory
                .fromQuery("$artist - $title", emptyList(), "")
            val results = SearchInfo.getInfo(youtubeService, queryHandler)
            val item = results.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .firstOrNull { it.duration > 60 }
            item?.url

        } catch (e: Exception) {
            Log.e("NewPipeExtractorImpl", "NewPipe search failed", e)
            null
        }
    }

    fun extractDirect(url: String): ExtractorResult.Success? {
        return try {
            val streamInfo = StreamInfo.getInfo(youtubeService, url)

            val audioStream = streamInfo.audioStreams
                .filter { it.format == MediaFormat.M4A || it.format == MediaFormat.WEBM }
                .maxByOrNull { it.averageBitrate }
                ?: return null

            ExtractorResult.Success(
                streamUrl = audioStream.content,
                expiresAt = System.currentTimeMillis() + 6 * 3600 * 1000L,
                sourceUrl = url  // save this in Room to skip search next time
            )
        } catch (e: ExtractionException) {
            Log.e("NewPipeExtractorImpl", "Extraction failed", e)
            null
        } catch (e: IOException) {
            Log.e("NewPipeExtractorImpl", "Network error", e)
            null
        }
    }

}