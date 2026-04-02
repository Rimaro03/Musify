package com.rimaro.musify.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentSearchBinding
import com.rimaro.musify.ui.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* TRENDINGS/GENRES LOGIC */
        val trendingArtistsRv = binding.searchTrendingArtistsRv
        val trendingArtistsAdapter = TrendingArtistsAdapter({})
        trendingArtistsRv.adapter = trendingArtistsAdapter
        trendingArtistsRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val genresRv = binding.searchGenresRv
        val genresAdapter = GenreAdapter({})
        genresRv.adapter = genresAdapter
        genresRv.layoutManager = GridLayoutManager(requireContext(), 2)

        observeTrendingUiState(trendingArtistsAdapter, genresAdapter)



        /* HISTORY LOGIC */
        val historyContainer = requireActivity().findViewById<LinearLayout>(R.id.searchview_history_container)
        val historyLayout = requireActivity().findViewById<LinearLayout>(R.id.searchview_history)
        val emptyHistoryLayout = requireActivity().findViewById<LinearLayout>(R.id.searchview_empty_history)

        observeHistory(historyLayout, emptyHistoryLayout, historyContainer)

        searchBar = requireActivity().findViewById(R.id.search_bar)
        searchView = requireActivity().findViewById(R.id.search_view)
        initSearchBar()
        searchView.editText.setOnEditorActionListener { view, actionId, event ->
            when(actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val query = searchView.text.toString()
                    searchBar.setText(query)
                    searchView.hide()

                    viewModel.onSearch(query)
                    viewModel.performSearch(query)

                    return@setOnEditorActionListener true
                }
            }
            false
        }

        /* SEARCH LOGIC */
        val searchResultsRv = binding.searchResultsRv
        val searchResultAdapter = SearchResultAdapter(viewModel::onClick, {}, {})
        searchResultsRv.adapter = searchResultAdapter
        searchResultsRv.layoutManager = LinearLayoutManager(requireContext())
        observeSearchUiState(searchResultAdapter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        clearSearchBar()
    }

    private fun observeTrendingUiState(
        artistAdapter: TrendingArtistsAdapter,
        genreAdapter: GenreAdapter
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.trendingUiState.collect { uiState ->
                when (uiState) {
                    is TrendingUiState.Idle -> {
                        binding.searchProgress.visibility = View.GONE
                        binding.searchArtistGenreContainer.visibility = View.GONE
                    }
                    is TrendingUiState.Loading -> {
                        binding.searchProgress.visibility = View.VISIBLE
                    }
                    is TrendingUiState.Success -> {
                        binding.searchProgress.visibility = View.GONE
                        binding.searchArtistGenreContainer.visibility = View.VISIBLE
                        artistAdapter.submitList(uiState.artists)
                        artistAdapter.notifyItemRangeChanged(0, uiState.artists.size)
                        genreAdapter.submitList(uiState.genres)
                        genreAdapter.notifyItemRangeChanged(0, uiState.genres.size)
                    }
                    is TrendingUiState.Error -> {
                        binding.searchProgress.visibility = View.GONE
                        Snackbar.make(binding.root, uiState.message, Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }

    private fun initSearchBar() {
        searchBar.visibility = View.VISIBLE
        searchView.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.searchbarFocused.collect {
                if(it) {
                    searchView.show()
                    searchView.editText.requestFocus()
                    sharedViewModel.consumeSearchbarFocused()
                }
            }
        }

        val navHostFragment =
            requireActivity().findViewById<View>(R.id.nav_host_fragment_content_main)
        val params = navHostFragment.layoutParams as? CoordinatorLayout.LayoutParams
        params?.behavior = AppBarLayout.ScrollingViewBehavior()
        navHostFragment.requestLayout()
    }

    private fun clearSearchBar() {
        val searchBar = requireActivity().findViewById<SearchBar>(R.id.search_bar)
        searchBar.visibility = View.GONE

        val searchView = requireActivity().findViewById<SearchView>(R.id.search_view)
        searchView.visibility = View.GONE

        val navHostFragment =
            requireActivity().findViewById<View>(R.id.nav_host_fragment_content_main)
        val params = navHostFragment.layoutParams as? CoordinatorLayout.LayoutParams
        params?.behavior = null
        navHostFragment.requestLayout()
    }

    private fun observeSearchUiState(adapter: SearchResultAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchUiState.collect { uiState ->
                    when (uiState) {
                        is SearchUiState.Idle -> {
                            binding.searchResultsRv.visibility = View.GONE
                        }

                        is SearchUiState.Loading -> {
                            binding.searchProgress.visibility = View.VISIBLE
                            binding.searchResultsRv.visibility = View.GONE
                            binding.searchArtistGenreContainer.visibility = View.GONE
                        }

                        is SearchUiState.Success -> {
                            binding.searchProgress.visibility = View.GONE
                            binding.searchResultsRv.visibility = View.VISIBLE
                            binding.searchArtistGenreContainer.visibility = View.GONE
                            adapter.submitList(uiState.searchResultLis)
                            adapter.notifyItemRangeChanged(0, uiState.searchResultLis.size)
                        }

                        is SearchUiState.Error -> {
                            binding.searchProgress.visibility = View.GONE
                            binding.searchResultsRv.visibility = View.GONE
                            binding.searchArtistGenreContainer.visibility = View.GONE
                            Snackbar.make(binding.root, uiState.message, Snackbar.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun observeHistory( historyLayout: LinearLayout, emptyHistoryLayout: LinearLayout, historyContainer: LinearLayout ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.history.collect { queries ->
                if(queries.isEmpty()) {
                    historyLayout.visibility = View.GONE
                    emptyHistoryLayout.visibility = View.VISIBLE
                } else {
                    historyLayout.visibility = View.VISIBLE
                    emptyHistoryLayout.visibility = View.GONE

                    historyContainer.removeAllViews()

                    queries.forEach { query ->
                        val item = layoutInflater.inflate(R.layout.item_history, historyContainer, false)
                        item.findViewById<TextView>(R.id.item_history_query).text = query
                        item.findViewById<ImageButton>(R.id.item_history_remove_btn).setOnClickListener {
                            viewModel.removeQuery(query)
                        }
                        item.setOnClickListener {
                            searchBar.setText(query)
                            searchView.hide()

                            viewModel.onSearch(query)
                            viewModel.performSearch(query)
                        }
                        historyContainer.addView(item)
                    }

                }
            }
        }
    }
}