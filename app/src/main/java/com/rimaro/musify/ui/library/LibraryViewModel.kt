package com.rimaro.musify.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.rimaro.musify.data.repository.RecentPlayedRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application,
    private val playerController: PlayerController,
    private val queueManager: QueueManager,
    private val recentPlayedRepository: RecentPlayedRepository
) : AndroidViewModel(application) {

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying

    // PLAYLIST IMPORTING METHODS //



}
