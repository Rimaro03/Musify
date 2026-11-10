package com.rimaro.musify.ui.library.followedArtists

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.rimaro.musify.R
import com.rimaro.musify.data.remote.deezer.dto.DeezerArtist
import com.rimaro.musify.databinding.FragmentArtistsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArtistsFragment : Fragment() {
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArtistsViewModel by viewModels()

    private var allItems: List<DeezerArtist> = emptyList()

    private lateinit var artistsProgress: ProgressBar
    private lateinit var artistsContent: LinearLayout
    private lateinit var artistsCount: TextView
    private lateinit var artistsRv: RecyclerView
    private lateinit var artistsAdapter: ArtistsAdapter
    private lateinit var artistsSearch: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        artistsProgress = binding.artistsProgress
        artistsContent = binding.artistsContent
        artistsCount = binding.artistsCount
        artistsRv = binding.artistsRv
        artistsAdapter = ArtistsAdapter(
            onArtistClick = {
//                findNavController().navigate(
//                    ArtistsFragmentDirections.
//                )
            }
        )
        artistsRv.adapter = artistsAdapter
        artistsRv.layoutManager = GridLayoutManager(requireContext(), 2)
        artistsSearch = binding.artistsSearchbar
        setupSearchBar()
        observeUiState()
    }

    private fun setupSearchBar() {
        artistsSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            artistsAdapter.submitList(
                if (query.isEmpty()) allItems
                else allItems.filter { it.name.contains(query, ignoreCase = true) }
            )
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.artistUiState.collect { state ->
                    when (state) {
                        is ArtistUiState.Success -> {
                            Log.d("ArtistsFragment", "Success")
                            artistsProgress.isVisible = false
                            artistsContent.isVisible = true
                            artistsCount.text =
                                getString(R.string.artists_count, state.artists.size)
                            artistsAdapter.submitList(state.artists)
                            allItems = state.artists
                        }

                        is ArtistUiState.Error -> {
                            Log.d("ArtistsFragment", "Err")
                            artistsProgress.isVisible = false
                            artistsContent.isVisible = false
                            Toast.makeText(
                                requireContext(),
                                "Failed to load artists",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is ArtistUiState.Loading -> {
                            Log.d("ArtistsFragment", "Load")
                            artistsProgress.isVisible = true
                            artistsContent.isVisible = false
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