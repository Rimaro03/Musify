package com.rimaro.musify.ui.player

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class TouchHelper(
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDropped: (from: Int, to: Int) -> Unit,
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
) {
    private var dragFrom = RecyclerView.NO_POSITION
    private var dragTo = RecyclerView.NO_POSITION

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) : Boolean {
        val from = viewHolder.adapterPosition
        dragFrom = from
        val to = target.adapterPosition
        dragTo = to
        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
        onMove(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)  = Unit

    override fun isLongPressDragEnabled() = true

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (dragFrom != RecyclerView.NO_POSITION && dragFrom != dragTo) {
            onDropped(dragFrom, dragTo)
        }
        dragFrom = RecyclerView.NO_POSITION
        dragTo = RecyclerView.NO_POSITION
    }
}