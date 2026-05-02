package com.rimaro.musify.util

import java.io.InputStream

data class CsvTrack(
    val title: String,
    val artist: String,
    val album: String?,
)

fun parseCsvStream(inputStream: InputStream): Sequence<CsvTrack> = sequence {
    inputStream.bufferedReader().useLines { lines ->
        lines.drop(1) // skip header
            .filter { it.isNotBlank() }
            .forEach { line ->
                val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
                if (cols.size >= 2) {
                    yield(CsvTrack(
                        title  = cols[1],
                        artist = cols[3],
                        album  = cols.getOrNull(2),
                    ))
                }
            }
    }
}