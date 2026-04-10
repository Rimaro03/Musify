package com.rimaro.musify.ui.player

import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class MiniplayerDragListener(
    private val onSwipeLeft: () -> Unit,
    private val onSwipeRight: () -> Unit,
) : View.OnTouchListener {

    private var downX = 0f
    private var translationX = 0f
    private val swipeThreshold = 200f
    private val clickThreshold = 20f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                translationX = view.translationX
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - downX
                view.translationX = translationX + deltaX
                view.alpha = 1 - (abs(view.translationX) / (view.width * 0.8f))
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val deltaX = event.rawX - downX

                when {
                    abs(deltaX) < clickThreshold -> {
                        view.performClick()
                    }
                    deltaX > swipeThreshold -> {
                        animateOffScreen(view, view.width.toFloat()) {
                            onSwipeRight()
                            resetView(view)
                        }
                    }
                    deltaX < -swipeThreshold -> {
                        animateOffScreen(view, -view.width.toFloat()) {
                            onSwipeLeft()
                            resetView(view)
                        }
                    }
                    else -> {
                        // Not enough swipe — snap back
                        ObjectAnimator.ofFloat(view, "translationX", 0f).apply {
                            duration = 200
                            start()
                        }
                        ObjectAnimator.ofFloat(view, "alpha", 1f).apply {
                            duration = 200
                            start()
                        }
                    }
                }
            }
        }
        return true
    }

    private fun animateOffScreen(view: View, targetX: Float, endAction: () -> Unit) {
        view.animate()
            .translationX(targetX)
            .alpha(0f)
            .setDuration(200)
            .withEndAction(endAction)
            .start()
    }

    private fun resetView(view: View) {
        view.translationX = 0f
        view.alpha = 1f
    }
}