package com.rimaro.musify.ui.playlist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.ui.search.SearchResultAdapter
import com.rimaro.musify.ui.search.SearchResultItem

class SwipeToQueueCallback(
    private val adapter: PlaylistTrackAdapter,
    private val onSwiped: (track: Track) -> Unit,
    private val queueIcon: Drawable?,
    private val context: Context
) : ItemTouchHelper.SimpleCallback(
    0, // no drag directions
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {
    val color = MaterialColors.getColor(context, R.attr.colorSurfaceVariant, Color.WHITE)
    val background = color.toDrawable()
    val maxSwipePx = 350F

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if(position == RecyclerView.NO_POSITION) {
            resetRow(viewHolder)
            return
        }
        val track = adapter.currentList[position].track
        onSwiped(track)

        Snackbar.make(viewHolder.itemView, "${track.title} is next up", Snackbar.LENGTH_SHORT).show()
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val iconMargin = (itemView.height - (queueIcon?.intrinsicHeight ?: 0)) / 2

        val clampedDx = if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            dX.coerceIn(-maxSwipePx, maxSwipePx)
        } else dX

        if (clampedDx == 0f) {
            clearView(recyclerView, viewHolder)
            return
        }

        if (clampedDx > 0) { // swiping right
            background.setBounds(itemView.left, itemView.top, clampedDx.toInt(), itemView.bottom)
            background.draw(c)
            queueIcon?.setBounds(
                itemView.left + iconMargin,
                itemView.top + iconMargin,
                itemView.left + iconMargin + (queueIcon.intrinsicWidth),
                itemView.bottom - iconMargin
            )
        } else { // swiping left
            background.setBounds(itemView.right + clampedDx.toInt(), itemView.top, itemView.right, itemView.bottom)
            background.draw(c)
            queueIcon?.setBounds(
                itemView.right - iconMargin - (queueIcon.intrinsicWidth ?: 0),
                itemView.top + iconMargin,
                itemView.right - iconMargin,
                itemView.bottom - iconMargin
            )
        }

        queueIcon?.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        resetRow(viewHolder)
    }

    private fun resetRow(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.translationX = 0f
        viewHolder.itemView.alpha = 1f
    }
}