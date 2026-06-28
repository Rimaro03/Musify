package com.rimaro.musify.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.rimaro.musify.R
import com.rimaro.musify.databinding.LayoutMiniplayerBinding
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.ui.common.PlayButtonState
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

    fun setButtonState(buttonState: PlayButtonState) {
        val progressDrawable = CircularProgressDrawable(binding.root.context).apply {
            setStyle(CircularProgressDrawable.DEFAULT)
            setColorSchemeColors(Color.WHITE)
            strokeWidth = 5f
            centerRadius = 20f
            start()
        }

        binding.miniplayerPlayPause.icon = when(buttonState) {
            is PlayButtonState.Buffering -> progressDrawable
            is PlayButtonState.PlayingThis -> ContextCompat.getDrawable(binding.root.context, R.drawable.pause_24px)
            else -> ContextCompat.getDrawable(binding.root.context, R.drawable.play_arrow_24px)
        }

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

    private var isSwipeDetected = false

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

                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > SWIPE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                ) {
                    isSwipeDetected = true  // Flag the swipe
                    if (diffX < 0) onSwipeLeft?.invoke()
                    else onSwipeRight?.invoke()
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Only allow click if no swipe was detected
                if (!isSwipeDetected) {
                    performClick()
                }
                return true
            }

            override fun onDown(e: MotionEvent) = true
        }
    )

    private var initialX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val diffX = event.x - initialX
                translationX = diffX * 0.4f
            }
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                isSwipeDetected = false  // Reset flag on new touch
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Reset flag and snap back
                isSwipeDetected = false
                animate()
                    .translationX(0f)
                    .setDuration(300)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }
        }
        return true  // Consume the event
    }

    fun setOnSwipeLeft(action: () -> Unit) { onSwipeLeft = action }
    fun setOnSwipeRight(action: () -> Unit) { onSwipeRight = action }
}