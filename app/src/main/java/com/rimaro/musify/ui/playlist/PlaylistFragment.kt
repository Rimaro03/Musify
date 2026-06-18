package com.rimaro.musify.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentPlaylistBinding
import com.rimaro.musify.domain.model.FirestorePlaylist
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.ui.common.TrackOptionsBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class PlaylistFragment : Fragment() {
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlaylistViewModel by viewModels()
    private val args: PlaylistFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val playlistId = args.playlistId
        viewModel.retrieveTrackIds(playlistId)

        val trackRv = binding.playlistTrackRv
        val trackAdapter = PlaylistTrackAdapter(
            {track -> viewModel.playTrack(track, playlistId)},
            {track -> showTrackMenu(track, playlistId)},
            viewModel::playPreview,
        )
        trackRv.adapter = trackAdapter
        trackRv.layoutManager = LinearLayoutManager(requireContext())
        observePlayerUiState(trackAdapter)

        val shuffleBtn = binding.playlistShuffleBtn
        shuffleBtn.setOnClickListener { viewModel.toggleShuffle() }
        observeShuffleMode(shuffleBtn)

        val playPlaylistBtn = binding.playlistPlayBtn
        playPlaylistBtn.setOnClickListener { viewModel.togglePlayButton(playlistId) }
        observePlayerState(playPlaylistBtn, playlistId)
    }

    private fun showTrackMenu(track: Track, playlistId: String?) {
        TrackOptionsBottomSheet.newInstance(track, playlistId)
            .show(childFragmentManager, "TrackOptionsBottomSheet")
    }

    private fun observePlayerUiState(adapter: PlaylistTrackAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlistUiState.collect { uiState ->
                    val progress = binding.playlistProgress
                    val container = binding.libraryContainer

                    when (uiState) {
                        is PlaylistUiState.Error -> {
                            progress.isVisible = false
                            container.isVisible = false
                            Snackbar.make(binding.root, uiState.message, Snackbar.LENGTH_LONG)
                                .show()
                        }
                        is PlaylistUiState.Success -> {
                            progress.isVisible = false
                            container.isVisible = true
                            setupPlaylistHeader(uiState.playlist, uiState.trackList)
                            adapter.submitList(uiState.trackList)
                        }
                        is PlaylistUiState.Loading -> {
                            progress.isVisible = true
                            container.isVisible = false
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupPlaylistHeader(playlist: FirestorePlaylist, tracks: List<Track>) {
        val cover = binding.playlistCover
        val title = binding.playlistTitle
        Glide.with(requireContext())
            .load(playlist.thumbnailPath)
            .centerCrop()
            .into(cover)
        title.text = playlist.name

        val tracksCount = binding.playlistTrackCount
        val trackHr = binding.playlistTrackHr
        val trackMin = binding.playlistTrackMin
        tracksCount.text = getString(R.string.track_count, playlist.trackIds.size)

        val tracksTotalMillis = tracks.sumOf { it.durationMs }
        val hours = tracksTotalMillis / 3600000
        val minutes = (tracksTotalMillis % 3600000) / 60000
        trackHr.text = getString(R.string.track_hours, hours)
        trackMin.text = getString(R.string.track_mins, minutes)
        if(hours == 0L) {
            trackHr.isVisible = false
        }
    }

    private fun observeShuffleMode(shuffleBtn: ImageButton) {
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shuffleEnabled.collect { enabled ->
                    val icon = if (enabled) {
                        androidx.media3.session.R.drawable.media3_icon_shuffle_on
                    } else {
                        androidx.media3.session.R.drawable.media3_icon_shuffle_off
                    }
                    shuffleBtn.setImageResource(icon)
                }
            }
        }
    }

    private fun observePlayerState(playPlaylistBtn: MaterialButton, playlistId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isPlaying.collect { isPlaying ->
                    playPlaylistBtn.icon = AppCompatResources.getDrawable(
                        requireContext(),
                        if(isPlaying && playlistId == viewModel.playingPlaylistId.value) {
                            R.drawable.pause_24px
                        }
                        else R.drawable.play_arrow_24px
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}