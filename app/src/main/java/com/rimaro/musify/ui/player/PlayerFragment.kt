package com.rimaro.musify.ui.player

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentPlayerBinding
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.ui.common.PlayButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayerViewModel by activityViewModels()

    private lateinit var seekBar: AppCompatSeekBar
    private lateinit var tvPosition: TextView
    private lateinit var tvDuration: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    private val updateProgressAction = object : Runnable {
        override fun run() {
            updateSeekBar()
            handler.postDelayed(this, 200L)
        }
    }

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
        addPlayerListener()
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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateSeekBar()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val duration = viewModel.trackDuration
                if (duration != C.TIME_UNSET) {
                    seekBar.max = duration.toInt()
                    tvDuration.text = formatTime(duration)
                }
            }
        }
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

    private fun addPlayerListener() {
        seekBar = binding.playerTrackSeekbar
        tvPosition = binding.playerTrackPosition
        tvDuration = binding.playerTrackDuration

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvPosition.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                isUserSeeking = true
                stopProgressUpdates()
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
                isUserSeeking = false
                viewModel.seekTo(sb.progress.toLong())
                if (viewModel.isPlaying.value) startProgressUpdates()
            }
        })

        viewModel.addListener(playerListener)
    }

    private fun startProgressUpdates() {
        handler.removeCallbacks(updateProgressAction)
        handler.post(updateProgressAction)
    }

    private fun stopProgressUpdates() {
        handler.removeCallbacks(updateProgressAction)
    }

    private fun updateSeekBar() {
        if (isUserSeeking) return
        val duration = viewModel.trackDuration
        val position = viewModel.trackCurrPos

        if (duration != C.TIME_UNSET && duration > 0) {
            seekBar.max = duration.toInt()
            tvDuration.text = formatTime(duration)
        }
        seekBar.progress = position.toInt()
        tvPosition.text = formatTime(position)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdates()
        _binding = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.removeListener(playerListener)
        stopProgressUpdates()
    }
}

