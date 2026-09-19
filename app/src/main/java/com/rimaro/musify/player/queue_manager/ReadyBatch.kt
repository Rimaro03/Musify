package com.rimaro.musify.player.queue_manager

import com.rimaro.musify.domain.model.Track

data class ReadyBatch(val localGeneration: Int, val tracks: List<Track>)