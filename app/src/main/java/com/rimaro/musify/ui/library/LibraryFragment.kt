package com.rimaro.musify.ui.library

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rimaro.musify.databinding.FragmentLibraryBinding
import com.rimaro.musify.ui.PlaybackViewmodel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LibraryViewModel by activityViewModels()
    private val playbackViewmodel: PlaybackViewmodel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.newPlaylistFab.setOnClickListener {
            NewPlaylistBottomSheet().show(childFragmentManager, "NewPlaylistBottomSheet")
        }

        val libraryRv = binding.libraryRv
        val libraryAdapter = LibraryAdapter(
            playbackViewmodel::playPlaylist,
            ::navigateToPlaylist
        )
        libraryRv.adapter = libraryAdapter
        libraryRv.layoutManager = GridLayoutManager(requireContext(), 2)
        observeLibraryUiState(libraryAdapter)
        observePlayerState(libraryAdapter)

        observeImportStatus()
    }

    private fun observeLibraryUiState(libraryAdapter: LibraryAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.libraryUiState.collect { state ->
                when(state) {
                    is LibraryUiState.Success -> {
                        binding.libraryProgress.isVisible = false
                        binding.libraryContent.isVisible = true
                        libraryAdapter.submitList(state.res)
                    }
                    is LibraryUiState.Error -> {
                        binding.libraryProgress.isVisible = false
                        binding.libraryContent.isVisible = false
                        Toast.makeText(requireContext(), "Failed to load playlists", Toast.LENGTH_SHORT).show()
                    }
                    is LibraryUiState.Loading -> {
                        binding.libraryProgress.isVisible = true
                        binding.libraryContent.isVisible = false
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observePlayerState(libraryAdapter: LibraryAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            playbackViewmodel.fetchingTracks.collect { fetchingTracks ->
                playbackViewmodel.playingPlaylistId?.let { playlistId ->
                    libraryAdapter.setPlayingPlaylistId(playlistId, fetchingTracks)
                }
            }
        }
    }

    private fun observeImportStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.importState.collect { state ->
                when(state) {
                    is ImportResult.Progress -> {
                        Toast.makeText(requireContext(), "Importing tracks... ", Toast.LENGTH_SHORT).show()
                    }
                    is ImportResult.Success -> {
                        Toast.makeText(requireContext(), "Import finished: ${state.imported} success, ${state.skipped} fails",
                            Toast.LENGTH_SHORT).show()
                    }
                    is ImportResult.Error -> {
                        Toast.makeText(requireContext(), "Importing failes", Toast.LENGTH_SHORT).show()

                        Log.e("LibraryFragment", "Track import failes: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToPlaylist(playlistId: String) {
        val action = LibraryFragmentDirections
            .actionLibraryFragmentToPlaylistFragment2(
                playlistId = playlistId
            )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}