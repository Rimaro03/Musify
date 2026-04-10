package com.rimaro.musify.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.rimaro.musify.R
import com.rimaro.musify.databinding.LayoutMiniplayerBinding
import com.rimaro.musify.domain.model.Track
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MiniPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : MaterialCardView(context, attrs) {
    private val binding = LayoutMiniplayerBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun bind(track: Track) {
        binding.miniplayerTrackName.text = track.title
        binding.miniplayerArtist.text = track.artist
        Glide.with(context)
            .load(track.artworkUrl)
            .into(binding.miniplayerTrackIcon)
    }


    fun setPlaying(isPlaying: Boolean) {
        binding.miniplayerPlayPause.icon = AppCompatResources.getDrawable(
            context,
            if(isPlaying) R.drawable.pause_24px
            else R.drawable.play_arrow_24px
        )
    }

    fun setOnPlayPauseClick(action: () -> Unit) {
        binding.miniplayerPlayPause.setOnClickListener { action() }
    }

    fun setOnSkipClick(action: () -> Unit) {
        //binding.btnSkipNext.setOnClickListener { action() }
    }

    fun setOnClick(action: () -> Unit) {
        binding.root.setOnClickListener { action() }
    }
}