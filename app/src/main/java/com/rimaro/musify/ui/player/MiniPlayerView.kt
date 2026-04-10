package com.rimaro.musify.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.appcompat.content.res.AppCompatResources
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.rimaro.musify.R
import com.rimaro.musify.databinding.LayoutMiniplayerBinding
import com.rimaro.musify.domain.model.Track
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
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
        setOnClickListener { action() }
    }

    private var onSwipeLeft: (() -> Unit)? = null
    private var onSwipeRight: (() -> Unit)? = null

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {

            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)

                // make sure it's a horizontal swipe, not vertical
                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > SWIPE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    if (diffX < 0) onSwipeLeft?.invoke()   // swipe left → skip next
                    else onSwipeRight?.invoke()             // swipe right → skip previous
                    return true
                }
                return false
            }

            override fun onDown(e: MotionEvent) = true

        }
    )

    private var initialX = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("MiniPlayer", "onTouchEvent: ${event.action}")
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val diffX = event.x - initialX
                // only translate horizontally, with some resistance
                translationX = diffX * 0.4f
            }
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // snap back
                animate()
                    .translationX(0f)
                    .setDuration(300)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }
        }
        return super.onTouchEvent(event)
    }

    fun setOnSwipeLeft(action: () -> Unit) { onSwipeLeft = action }
    fun setOnSwipeRight(action: () -> Unit) { onSwipeRight = action }
}