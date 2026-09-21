package com.rimaro.musify.ui.common.trackOptionsSheet

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.session.R
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rimaro.musify.NavGraphDirections
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.databinding.FragmentTrackOptionsBinding
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toFirestoreTrack
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.controller.PreviewPlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
import com.rimaro.musify.ui.common.model.TrackUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackOptionsSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentTrackOptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrackOptionsViewModel by viewModels()

    @Inject lateinit var queueManager: QueueManager
    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var previewPlayerController: PreviewPlayerController
    @Inject lateinit var firestoreLikedTracksRepo: FirestoreLikedTracksRepo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrackOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { trackUiModel ->
                    trackUiModel?.let { setupBottomSheet(it) }
                }
            }
        }
    }

    private fun setupBottomSheet(trackModel: TrackUiModel) {
        val track = trackModel.track

        // track metadata
        binding.trackOptTrackName.text = track.title
        binding.trackOptTrackArtist.text = track.artist

        // top buttons
        binding.trackOptPlayNext.setOnClickListener {
            val currTrack = playerController.currentTrack.value
            if(currTrack == null) {
                queueManager.loadQueue(listOf(track), playerController.shuffleEnabled.value)
            } else {
                queueManager.playNext(currTrack, track)
            }
            dismiss()
        }
        binding.trackOptLike.setImageResource(
            if (trackModel.isLiked) R.drawable.media3_icon_heart_filled
            else R.drawable.media3_icon_heart_unfilled
        )
        binding.trackOptLike.setOnClickListener {
            if (trackModel.isLiked) {
                firestoreLikedTracksRepo.removeTrack(track.id)
            } else {
                firestoreLikedTracksRepo.addTrack(track.toFirestoreTrack())
            }
            dismiss()
        }
        binding.trackOptShare.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out this track: https://www.deezer.com/track/${track.id}")
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)

            dismiss()
        }

        // list buttons
        binding.trackOptAddToQueue.setOnClickListener {
            queueManager.enqueue(track)
            dismiss()
        }
        binding.trackOptSaveToPlaylist.setOnClickListener {
            findNavController().navigate(
                TrackOptionsSheetDirections.actionTrackOptionsToAddToPlaylist(track.id)
            )
            dismiss()
        }
        binding.trackOptGotoAlbum.setOnClickListener {  }
        binding.trackOptGotoArtist.setOnClickListener {  }

        binding.trackOptPreviewBtn.setOnClickListener {
            playPreview(track)
        }
        binding.trackOptDismissBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun playPreview(track: Track) {
        playerController.pause()
        track.previewUrl?.let {
            previewPlayerController.playPreview(track.id.toString(), it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}