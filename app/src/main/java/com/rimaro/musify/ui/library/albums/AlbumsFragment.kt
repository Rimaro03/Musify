package com.rimaro.musify.ui.library.albums

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.rimaro.musify.R
import com.rimaro.musify.data.remote.deezer.dto.DeezerAlbum
import com.rimaro.musify.databinding.FragmentAlbumsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlbumsFragment : Fragment() {
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlbumsViewModel by viewModels()

    private var allItems: List<DeezerAlbum> = emptyList()

    private lateinit var albumsProgress: ProgressBar
    private lateinit var albumsContent: LinearLayout
    private lateinit var albumsCount: TextView
    private lateinit var albumsRv: RecyclerView
    private lateinit var albumsAdapter: AlbumsAdapter
    private lateinit var albumsSearch: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        albumsProgress = binding.albumsProgress
        albumsContent = binding.albumsContent
        albumsCount = binding.albumsCount
        albumsRv = binding.albumsRv
        albumsAdapter = AlbumsAdapter(
            onAlbumClick = {
//                findNavController().navigate(
//                    albumsFragmentDirections.
//                )
            }
        )
        albumsRv.adapter = albumsAdapter
        albumsRv.layoutManager = GridLayoutManager(requireContext(), 2)
        albumsSearch = binding.albumsSearchbar
        setupSearchBar()
        observeUiState()
    }

    private fun setupSearchBar() {
        albumsSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            albumsAdapter.submitList(
                if (query.isEmpty()) allItems
                else allItems.filter { it.title.contains(query, ignoreCase = true) }
            )
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.albumsUiState.collect { state ->
                    when (state) {
                        is AlbumsUiState.Success -> {
                            Log.d("ArtistsFragment", "Success")
                            albumsProgress.isVisible = false
                            albumsContent.isVisible = true
                            albumsCount.text =
                                getString(R.string.artists_count, state.albums.size)
                            albumsAdapter.submitList(state.albums)
                            allItems = state.albums
                        }

                        is AlbumsUiState.Error -> {
                            Log.d("ArtistsFragment", "Err")
                            albumsProgress.isVisible = false
                            albumsContent.isVisible = false
                            Toast.makeText(
                                requireContext(),
                                "Failed to load albums",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is AlbumsUiState.Loading -> {
                            Log.d("ArtistsFragment", "Load")
                            albumsProgress.isVisible = true
                            albumsContent.isVisible = false
                        }

                        else -> {
                            Log.d("ArtistsFragment", "Idle")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}