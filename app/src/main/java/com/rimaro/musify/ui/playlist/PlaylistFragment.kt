package com.rimaro.musify.ui.playlist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.rimaro.musify.NavGraphDirections
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentPlaylistBinding
import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist
import com.rimaro.musify.ui.common.PlayButtonState
import com.rimaro.musify.domain.model.TrackUiModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class PlaylistFragment : Fragment() {
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var trackRv: RecyclerView
    private lateinit var trackAdapter: PlaylistTrackAdapter

    private val viewModel: PlaylistViewModel by viewModels()
    private val args: PlaylistFragmentArgs by navArgs()

    private var isToolbarTitleVisible = false

    private lateinit var toolbarTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val playlistId = args.playlistId

        trackRv = binding.playlistTrackRv
        trackAdapter = PlaylistTrackAdapter(
            { trackModel -> viewModel.playTrack(trackModel.track) },
            { trackModel -> showTrackMenu(trackModel, playlistId) },
            { trackModel -> showTrackMenu(trackModel, playlistId) }, //viewModel.playPreview(trackModel.track)
            { trackUiModel -> viewModel.unlikeTrack(trackUiModel.track)}
        )
        trackRv.adapter = trackAdapter
        trackRv.layoutManager = LinearLayoutManager(requireContext())
        setupSwipe()
        observePlayerUiState()

        val shuffleBtn = binding.playlistShuffleBtn
        shuffleBtn.setOnClickListener { viewModel.toggleShuffle() }
        observeShuffleMode(shuffleBtn)

        val playPlaylistBtn = binding.playlistPlayBtn
        playPlaylistBtn.setOnClickListener { viewModel.togglePlayButton() }
        observePlayerState(playPlaylistBtn)

        toolbarTitle = requireActivity().findViewById(R.id.toolbar_title)
        toolbarTitle.isVisible = true
        binding.libraryContainer.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                updateToolbarTitleVisibility(scrollY)
            }
        )
    }

    private fun showTrackMenu(trackModel: TrackUiModel, playlistId: String?) {
        findNavController().navigate(
            NavGraphDirections.actionGlobalTrackOptionsSheet(trackModel.track.id, playlistId)
        )
    }

    private fun setupSwipe() {
        val queueIcon = ContextCompat.getDrawable(requireContext(), R.drawable.low_priority_24px)

        val swipeCallback = SwipeToQueueCallback(
            adapter = trackAdapter,
            onSwiped = { track -> viewModel.playNext(track) },
            queueIcon = queueIcon,
            requireContext()
        )

        ItemTouchHelper(swipeCallback).attachToRecyclerView(trackRv)
    }

    private fun observePlayerUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    val progress = binding.playlistProgress
                    val container = binding.libraryContainer

                    when (uiState) {
                        is PlaylistUiState.Error -> {
                            progress.isVisible = false
                            container.isVisible = false
                            Snackbar.make(binding.root, uiState.message, Snackbar.LENGTH_LONG)
                                .show()
                        }
                        is PlaylistUiState.Success -> {
                            progress.isVisible = false
                            container.isVisible = true
                            setupPlaylistHeader(uiState.playlist, uiState.trackList)
                            trackAdapter.submitList(uiState.trackList)
                        }
                        is PlaylistUiState.Loading -> {
                            progress.isVisible = true
                            container.isVisible = false
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupPlaylistHeader(playlist: FirestorePlaylist, trackModels: List<TrackUiModel>) {
        val tracks = trackModels.map { it.track }
        val cover = binding.playlistCover
        val title = binding.playlistTitle
        Glide.with(requireContext())
            .load(playlist.thumbnailPath)
            .centerCrop()
            .into(cover)
        title.text = playlist.name

        val tracksCount = binding.playlistTrackCount
        val trackHr = binding.playlistTrackHr
        val trackMin = binding.playlistTrackMin
        tracksCount.text = getString(R.string.track_count, playlist.tracks.size)

        val tracksTotalMillis = tracks.sumOf { it.durationMs }
        val hours = tracksTotalMillis / 3600000
        val minutes = (tracksTotalMillis % 3600000) / 60000
        trackHr.text = getString(R.string.track_hours, hours)
        trackMin.text = getString(R.string.track_mins, minutes)
        if(hours == 0L) {
            trackHr.isVisible = false
        }
    }

    private fun observeShuffleMode(shuffleBtn: ImageButton) {
        viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shuffleEnabled.collect { enabled ->
                    val icon = if (enabled) {
                        androidx.media3.session.R.drawable.media3_icon_shuffle_on
                    } else {
                        androidx.media3.session.R.drawable.media3_icon_shuffle_off
                    }
                    shuffleBtn.setImageResource(icon)
                }
            }
        }
    }

    private fun observePlayerState(playPlaylistBtn: MaterialButton) {
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
                    playPlaylistBtn.icon = when(buttonState) {
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

    private fun updateToolbarTitleVisibility(scrollY: Int) {
        val titleBottom = binding.playlistTitle.bottom

        // simpler/more robust: compare scrollY to the title's top offset
        val shouldShowToolbarTitle = scrollY >= titleBottom

        if (shouldShowToolbarTitle != isToolbarTitleVisible) {
            isToolbarTitleVisible = shouldShowToolbarTitle
            animateToolbarTitle(shouldShowToolbarTitle)
        }
    }

    private fun animateToolbarTitle(show: Boolean) {
        val toolbarTitleView = requireActivity().findViewById<TextView>(R.id.toolbar_title)

        toolbarTitleView.animate()
            .alpha(if (show) 1f else 0f)
            .setDuration(150)
            .withStartAction {
                if (show) {
                    toolbarTitleView.text = binding.playlistTitle.text
                    toolbarTitleView.visibility = View.VISIBLE
                }
            }
            .withEndAction {
                if (!show) toolbarTitleView.visibility = View.INVISIBLE
            }
            .start()
    }

    override fun onDestroy() {
        toolbarTitle.text = ""
        toolbarTitle.alpha = 0f
        toolbarTitle.visibility = View.GONE
        _binding = null
        super.onDestroy()
    }
}