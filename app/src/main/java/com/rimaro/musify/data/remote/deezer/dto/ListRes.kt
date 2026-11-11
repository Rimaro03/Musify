package com.rimaro.musify.data.remote.deezer.dto

import kotlinx.serialization.Serializable

@Serializable
data class ListRes<T> (
    val data: List<T> = emptyList()
)
