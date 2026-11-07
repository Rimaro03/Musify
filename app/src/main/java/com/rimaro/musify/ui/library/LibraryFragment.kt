package com.rimaro.musify.ui.library

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rimaro.musify.databinding.FragmentLibraryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var likedTracks: LinearLayout
    private lateinit var playlists: LinearLayout
    private lateinit var artists: LinearLayout
    private lateinit var albums: LinearLayout

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

        val libraryRv = binding.libraryRv
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}