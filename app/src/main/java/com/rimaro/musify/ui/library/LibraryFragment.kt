package com.rimaro.musify.ui.library

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentLibraryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class LibraryFragment : Fragment(), MenuProvider {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LibraryViewModel by activityViewModels()
    //private val playbackViewmodel: PlaybackViewmodel by activityViewModels()

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

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val libraryRv = binding.libraryRv
        val libraryAdapter = LibraryAdapter(
            viewModel::togglePlayButton,
            ::navigateToPlaylist
        )
        libraryRv.adapter = libraryAdapter
        libraryRv.layoutManager = GridLayoutManager(requireContext(), 2)
        observeLibraryUiState(libraryAdapter)
        observePlayerState(libraryAdapter)

        observeImportStatus()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_library_fragment, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.library_add -> {
                NewPlaylistBottomSheet().show(childFragmentManager, "NewPlaylistBottomSheet")
                true
            }
            else -> false
        }
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
            viewModel.playingPlaylistId.collect {
                libraryAdapter.setPlayingPlaylistId(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPlaying.collect {
                libraryAdapter.setIsPlaying(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.playerState.collect {
                libraryAdapter.setPlayerState(it)
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