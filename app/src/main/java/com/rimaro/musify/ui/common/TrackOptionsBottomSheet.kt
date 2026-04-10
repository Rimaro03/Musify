package com.rimaro.musify.ui.common

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rimaro.musify.databinding.FragmentTrackOptionsBinding
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.player.controller.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentTrackOptionsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var playerController: PlayerController

    // pass the track via companion object
    companion object {
        private const val ARG_TRACK = "track"
        fun newInstance(track: Track) = TrackOptionsBottomSheet().apply {
            arguments = Bundle().apply { putParcelable(ARG_TRACK, track) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrackOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val track = arguments?.getParcelable<Track>(ARG_TRACK, Track::class.java) ?: return

        binding.trackOptPlayNext.setOnClickListener {
            playerController.enqueueTracks(listOf(track), 1)
            dismiss()
        }

        binding.trackOptLike.setOnClickListener {  }

        binding.trackOptShare.setOnClickListener {  }

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