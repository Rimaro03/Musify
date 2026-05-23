package com.rimaro.musify.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rimaro.musify.databinding.FragmentPlaylistBinding
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
            viewModel::playTrack,
            ::showTrackMenu
        )
        trackRv.adapter = trackAdapter
        trackRv.layoutManager = LinearLayoutManager(requireContext())
        observePlayerUiState(trackAdapter)
    }

    private fun showTrackMenu(track: Track) {
        TrackOptionsBottomSheet.newInstance(track)
            .show(childFragmentManager, "TrackOptionsBottomSheet")
    }

    private fun observePlayerUiState(adapter: PlaylistTrackAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlistUiState.collect { uiState ->
                    when (uiState) {
                        is PlaylistUiState.Error -> {
                            Snackbar.make(binding.root, uiState.message, Snackbar.LENGTH_LONG)
                                .show()
                        }
                        is PlaylistUiState.Success -> {
                            adapter.submitList(uiState.trackList)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}