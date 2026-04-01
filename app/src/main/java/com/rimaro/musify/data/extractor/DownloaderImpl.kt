package com.rimaro.musify.data.extractor

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import javax.inject.Inject

class DownloaderImpl @Inject constructor(
    private val client: OkHttpClient
) : Downloader() {

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .url(request.url())

        // 1. Add all headers EXCEPT Content-Type (which must be set on the body instead)
        request.headers().forEach { (key, values) ->
            if (!key.equals("Content-Type", ignoreCase = true)) {
                values.forEach { value -> builder.addHeader(key, value) }
            }
        }

        // 2. Handle HTTP methods
        when (request.httpMethod()) {
            "GET" -> builder.get()
            "POST" -> {
                val dataToSend = request.dataToSend() ?: ByteArray(0)

                // 3. Extract the Content-Type from NewPipe's headers, default to JSON
                val contentTypeHeader = request.headers()["Content-Type"]?.firstOrNull()
                    ?: "application/json"
                val mediaType = contentTypeHeader.toMediaTypeOrNull()

                // 4. Attach the MediaType properly to the RequestBody
                builder.post(dataToSend.toRequestBody(mediaType))
            }
            "HEAD" -> builder.head()
        }

        val response = client.newCall(builder.build()).execute()

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body?.string(),
            request.url()
        )
    }
}