package com.rimaro.musify.ui.library.likedTracks

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.search.SearchBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.rimaro.musify.NavGraphDirections
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentLikedTracksBinding
import com.rimaro.musify.domain.model.TrackUiModel
import com.rimaro.musify.ui.common.PlayButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LikedTracksFragment : Fragment() {
    private var _binding: FragmentLikedTracksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LikedTracksViewModel by viewModels()

    private var allItems: List<TrackUiModel> = emptyList()

    private lateinit var likedTracksContainer: LinearLayout
    private lateinit var likedTracksProgress: ProgressBar
    private lateinit var likedTracksRv: RecyclerView
    private lateinit var likedTracksAdapter: LikedTrackAdapter
    private lateinit var likedTracksCount: TextView
    private lateinit var likedTracksSearch: TextInputEditText
    private lateinit var playBtn: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLikedTracksBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        likedTracksContainer = binding.likedtracksContainer
        likedTracksProgress = binding.likedtracksProgress
        likedTracksRv = binding.likedtracksRv
        likedTracksAdapter = LikedTrackAdapter(
            onTrackClick = { viewModel.playFromTrack(it.track) },
            onTrackLongClick = ::showTrackMenu,
            onTrackLikeClick = { viewModel.unlikeTrack(it.track) },
            onMenuClick = ::showTrackMenu
        )
        likedTracksRv.adapter = likedTracksAdapter
        likedTracksRv.layoutManager = LinearLayoutManager(requireContext())
        likedTracksCount = binding.likedtracksCount
        likedTracksSearch = binding.likedtracksSearchbar
        playBtn = binding.likedtracksPlayBtn
        playBtn.setOnClickListener { viewModel.togglePlayButton() }

        observeUiState()
        setupSearchBar()
        observePlayerState()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when(uiState) {
                        is LikedTracksUiState.Success -> {
                            likedTracksProgress.isVisible = false
                            likedTracksContainer.isVisible = true
                            allItems = uiState.tracks
                            likedTracksCount.text = getString(R.string.liked_tracks, uiState.tracks.size)
                            likedTracksAdapter.submitList(uiState.tracks)
                        }
                        is LikedTracksUiState.Error -> {
                            likedTracksProgress.isVisible = false
                            likedTracksContainer.isVisible = false
                            Snackbar.make(binding.root, uiState.message, Snackbar.LENGTH_LONG)
                                .show()
                        }
                        is LikedTracksUiState.Loading -> {
                            likedTracksProgress.isVisible = true
                            likedTracksContainer.isVisible = false
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupSearchBar() {
        likedTracksSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            likedTracksAdapter.submitList(
                if (query.isEmpty()) allItems
                else allItems.filter { it.track.title.contains(query, ignoreCase = true) }
            )
        }
    }

    private fun showTrackMenu(trackModel: TrackUiModel) {
        findNavController().navigate(
            NavGraphDirections.actionGlobalTrackOptionsSheet(trackModel.track.id, "__liked__")
        )
    }

    private fun observePlayerState() {
        val progressDrawable = CircularProgressDrawable(binding.root.context).apply {
            setStyle(CircularProgressDrawable.DEFAULT)
            setColorSchemeColors(Color.BLACK)
            strokeWidth = 10f
            centerRadius = 0f
            start()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playButtonState.collect { buttonState ->
                    playBtn.icon = when(buttonState) {
                        PlayButtonState.Idle -> AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.play_arrow_24px
                        )

                        PlayButtonState.Buffering -> progressDrawable

                        PlayButtonState.PlayingThis -> AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.pause_24px
                        )

                        PlayButtonState.PlayingOther -> AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.play_arrow_24px
                        )
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