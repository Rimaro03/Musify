package com.rimaro.musify.ui.common

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.databinding.FragmentTrackOptionsBinding
import com.rimaro.musify.domain.model.toFirestoreTrack
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.ui.common.model.TrackUiModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentTrackOptionsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var firestoreLikedTracksRepo: FirestoreLikedTracksRepo

    companion object {
        private const val ARG_TRACK = "trackModel"
        private const val ARG_PLAYLIST_ID = "playlist_id"
        fun newInstance(trackModel: TrackUiModel, playlistId: String?) = TrackOptionsBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TRACK, trackModel)
                putString(ARG_PLAYLIST_ID, playlistId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrackOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val trackModel = arguments?.getParcelable(ARG_TRACK, TrackUiModel::class.java) ?: return
        val track = trackModel.track
        val playlistId = arguments?.getString(ARG_PLAYLIST_ID)

        // track metadata
        binding.trackOptTrackName.text = track.title
        binding.trackOptTrackArtist.text = track.artist

        // top buttons
        binding.trackOptPlayNext.setOnClickListener {
            playerController.enqueueTracks(listOf(track), 1)
            dismiss()
        }
        binding.trackOptLike.setImageResource(
            if (trackModel.isLiked) androidx.media3.session.R.drawable.media3_icon_heart_filled
            else androidx.media3.session.R.drawable.media3_icon_heart_unfilled
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
                putExtra(Intent.EXTRA_TEXT, "Check out this track: ${track.sourceUrl}")
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)

            dismiss()
        }

        // list buttons
        binding.trackOptAddToQueue.setOnClickListener {
            playerController.enqueueTracks(listOf(track))
            dismiss()
        }
        binding.trackOptSaveToPlaylist.setOnClickListener {  }
        binding.trackOptGotoAlbum.setOnClickListener {  }
        binding.trackOptGotoArtist.setOnClickListener {  }
        binding.trackOptDismissBtn.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}