package com.rimaro.musify.ui.library.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.rimaro.musify.databinding.FragmentPlaylistsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistsFragment : Fragment() {
    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlaylistsViewModel by viewModels()

    private lateinit var playlistsProgress: ProgressBar
    private lateinit var playlistsContent: NestedScrollView
    private lateinit var playlistsRv: RecyclerView
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var playlistsCount: TextView
    private lateinit var playlistsSearch: SearchBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistsProgress = binding.playlistsProgress
        playlistsContent = binding.playlistsContent
        playlistsRv = binding.playlistsRv
        playlistsCount = binding.playlistsCount
        playlistsSearch = binding.playlistsSearchbar

        playlistsAdapter = PlaylistsAdapter(
            onPlaylistClick = ::navigateToPlaylist,
            createThumbnail = viewModel::createThumbnail
        )
        playlistsRv.adapter = playlistsAdapter
        playlistsRv.layoutManager = GridLayoutManager(requireContext(), 2)
        observeUiState()
    }

    private fun navigateToPlaylist(playlistId: String) {
        val action = PlaylistsFragmentDirections
            .actionPlaylistsFragmentToPlaylistFragment(
                playlistId = playlistId
            )
        findNavController().navigate(action)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.libraryUiState.collect { state ->
                when(state) {
                    is PlaylistsUiState.Success -> {
                        playlistsProgress.isVisible = false
                        playlistsContent.isVisible = true
                        playlistsAdapter.submitList(state.res)
                    }
                    is PlaylistsUiState.Error -> {
                        playlistsProgress.isVisible = false
                        playlistsContent.isVisible = false
                        Toast.makeText(requireContext(), "Failed to load playlists", Toast.LENGTH_SHORT).show()
                    }
                    is PlaylistsUiState.Loading -> {
                        playlistsProgress.isVisible = true
                        playlistsContent.isVisible = false
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}