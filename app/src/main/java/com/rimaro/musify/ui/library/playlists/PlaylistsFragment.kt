package com.rimaro.musify.ui.library.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.rimaro.musify.R
import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist
import com.rimaro.musify.databinding.FragmentPlaylistsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistsFragment : Fragment(), MenuProvider {
    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlaylistsViewModel by viewModels()

    private var allItems: List<FirestorePlaylist> = emptyList()

    private lateinit var playlistsProgress: ProgressBar
    private lateinit var playlistsContent: LinearLayout
    private lateinit var playlistsRv: RecyclerView
    private lateinit var playlistsAdapter: PlaylistsAdapter
    private lateinit var playlistsCount: TextView
    private lateinit var playlistsSearch: TextInputEditText

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
        setupSearchBar()

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_playlists_fragment, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.library_add -> {
                findNavController().navigate(
                    PlaylistsFragmentDirections.actionPlaylistsFragmentToNewPlaylistSheet()
                )
                true
            }
            else -> false
        }
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
                        playlistsCount.text = getString(R.string.playlists_count, state.res.size)
                        playlistsAdapter.submitList(state.res)
                        allItems = state.res
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

    private fun setupSearchBar() {
        playlistsSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            playlistsAdapter.submitList(
                if (query.isEmpty()) allItems
                else allItems.filter { it.name.contains(query, ignoreCase = true) }
            )
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}