package com.rimaro.musify.ui.player

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentPlayerBinding
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.ui.common.PlayButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.timeago.patterns.fa

@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeCurrentTrack()
        setupItemsMenu()
    }


    private fun setupItemsMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_player_fragment, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun observeCurrentTrack() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentTrack.collect {
                    setupPlayer(it)
                }
            }
        }
    }

    private fun setupPlayer(track: Track?) {
        // track metadata
        val cover = binding.playerTrackCover
        Glide.with(requireContext())
            .load(track?.artworkUrl)
            .centerCrop()
            .into(cover)

        val trackTitle = binding.playerTrackTitle
        trackTitle.text = track?.title ?: "Unknown title"

        val trackArtists = binding.playerTrackArtist
        trackArtists.text = track?.artist ?: "Unknown artists"

        // playback controls
        val skipPrev = binding.playerSkipPrevBtn
        skipPrev.setOnClickListener { viewModel.skipPrevious() }

        val skipNext = binding.playerSkipNextBtn
        skipNext.setOnClickListener { viewModel.skipNext() }

        val playBtn = binding.playerPlayBtn
        playBtn.setOnClickListener {
            if(viewModel.isPlaying.value) {
                viewModel.pause()
            } else {
                viewModel.resume()
            }
        }
        val progressDrawable = CircularProgressDrawable(binding.root.context).apply {
            setStyle(CircularProgressDrawable.DEFAULT)
            setColorSchemeColors(Color.BLACK)
            strokeWidth = 5f
            centerRadius = 20f
            start()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playButtonState.collect { state ->
                    playBtn.icon = when(state) {
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

        val shuffleBtn = binding.playerShuffleBtn
        shuffleBtn.setOnClickListener { viewModel.toggleShuffle() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shuffleEnabled.collect { shuffle ->
                    shuffleBtn.icon = AppCompatResources.getDrawable(
                        requireContext(),
                        when(shuffle) {
                                true ->
                                    androidx.media3.session.R.drawable.media3_icon_shuffle_on
                                false ->
                                    androidx.media3.session.R.drawable.media3_icon_shuffle_off
                            }
                    )
                }
            }
        }

        val repeatBtn = binding.playerRepeatBtn
        repeatBtn.setOnClickListener { viewModel.toggleRepeatMode() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.repeatMode.collect { repeatMode ->
                    when(repeatMode) {
                        Player.REPEAT_MODE_OFF ->
                            repeatBtn.icon = AppCompatResources.getDrawable(
                                requireContext(),
                                androidx.media3.session.R.drawable.media3_icon_repeat_off
                            )
                        Player.REPEAT_MODE_ONE ->
                            repeatBtn.icon = AppCompatResources.getDrawable(
                                requireContext(),
                                androidx.media3.session.R.drawable.media3_icon_repeat_one
                            )
                        Player.REPEAT_MODE_ALL ->
                            repeatBtn.icon = AppCompatResources.getDrawable(
                                requireContext(),
                                androidx.media3.session.R.drawable.media3_icon_repeat_all
                            )
                        else -> {}
                    }

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

