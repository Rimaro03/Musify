package com.rimaro.musify.ui.common

// SwipeToQueueCallback.kt
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import androidx.core.graphics.drawable.toDrawable

class SwipeToQueueCallback(
    private val onSwiped: (position: Int) -> Unit,
    private val queueIcon: Drawable?,
    private val context: Context
) : ItemTouchHelper.SimpleCallback(
    0, // no drag directions
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {
    val color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, Color.WHITE)
    val background = color.toDrawable()

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false // no drag-and-drop

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwiped(viewHolder.adapterPosition)
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

        if (dX == 0f) {
            clearView(recyclerView, viewHolder)
            return
        }

        if (dX > 0) { // swiping right
            background.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
            background.draw(c)
            queueIcon?.setBounds(
                itemView.left + iconMargin,
                itemView.top + iconMargin,
                itemView.left + iconMargin + (queueIcon.intrinsicWidth),
                itemView.bottom - iconMargin
            )
        } else { // swiping left
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            background.draw(c)
            queueIcon?.setBounds(
                itemView.right - iconMargin - (queueIcon.intrinsicWidth ?: 0),
                itemView.top + iconMargin,
                itemView.right - iconMargin,
                itemView.bottom - iconMargin
            )
        }

        queueIcon?.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}