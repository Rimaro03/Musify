package com.rimaro.musify.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rimaro.musify.databinding.ItemPlaylistTrackBinding
import com.rimaro.musify.ui.common.model.TrackUiModel

class QueueAdapter (

): ListAdapter<TrackUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TrackUiModel>() {
            override fun areItemsTheSame(old: TrackUiModel, new: TrackUiModel) =
                old.track.id == new.track.id

            override fun areContentsTheSame(old: TrackUiModel, new: TrackUiModel) =
                old == new
        }
    }

    class ViewHolder(private val binding: ItemPlaylistTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            trackModel: TrackUiModel
        ) {}
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemPlaylistTrackBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val item = getItem(position)
        (holder as ViewHolder).bind(item)
    }
}