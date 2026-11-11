package com.rimaro.musify.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rimaro.musify.NavGraphDirections
import com.rimaro.musify.databinding.FragmentLibraryBinding
import com.rimaro.musify.domain.model.TrackUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LibraryViewModel by viewModels()

    private lateinit var likedTracks: LinearLayout
    private lateinit var playlists: LinearLayout
    private lateinit var artists: LinearLayout
    private lateinit var albums: LinearLayout
    private lateinit var recentPlayerdRv: RecyclerView
    private lateinit var recentPlayedAdapter: RecentPlayedAdapter

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

        likedTracks = binding.libraryLiked
        playlists = binding.libraryPlaylists
        artists = binding.libraryArtists
        albums = binding.libraryAlbums
        setupDestinations()

        recentPlayerdRv = binding.recentPlayedRv
        recentPlayedAdapter = RecentPlayedAdapter(
            onTrackClick = { viewModel.playTrack(it.track) },
            onTrackLongClick = ::showTrackMenu,
            onTrackLikeClick = { viewModel.unlikeTrack(it.track) },
            onMenuClick = ::showTrackMenu
        )
        recentPlayerdRv.adapter = recentPlayedAdapter
        recentPlayerdRv.layoutManager = LinearLayoutManager(requireContext())
        observeRecentPlayedTracks()
    }

    private fun setupDestinations() {
        likedTracks.setOnClickListener {
            findNavController().navigate(
                LibraryFragmentDirections.actionLibraryFragmentToLikedTracksFragment()
            )
        }
        playlists.setOnClickListener {
            findNavController().navigate(
                LibraryFragmentDirections.actionLibraryFragmentToPlaylistsFragment()
            )
        }
        artists.setOnClickListener {
            findNavController().navigate(
                LibraryFragmentDirections.actionLibraryFragmentToArtistsFragment()
            )
        }
        albums.setOnClickListener {
            findNavController().navigate(
                LibraryFragmentDirections.actionLibraryFragmentToAlbumsFragment()
            )
        }
    }

    private fun observeRecentPlayedTracks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    when(it) {
                        is LibraryUiState.Success -> {
                            recentPlayedAdapter.submitList(it.recentPlayedTracks)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showTrackMenu(trackModel: TrackUiModel) {
        findNavController().navigate(
            NavGraphDirections.actionGlobalTrackOptionsSheet(trackModel.track.id, "__liked__")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}