package com.rimaro.musify.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.ItemQueueTrackBinding
import com.rimaro.musify.ui.common.model.TrackUiModel

class QueueAdapter : ListAdapter<TrackUiModel, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TrackUiModel>() {
            override fun areItemsTheSame(old: TrackUiModel, new: TrackUiModel) =
                old.track.id == new.track.id

            override fun areContentsTheSame(old: TrackUiModel, new: TrackUiModel) =
                old == new
        }
    }

    class ViewHolder(private val binding: ItemQueueTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            trackModel: TrackUiModel
        ) {
            val track = trackModel.track

            // track metadata
            binding.queueTrackName.text = track.title
            binding.queueTrackArtist.text = track.artist
            Glide.with(binding.root)
                .load(track.artworkUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.queueTrackThumbnail)
            // highlight playing track
            binding.root.isActivated = trackModel.isPlaying
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemQueueTrackBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val item = getItem(position)
        (holder as ViewHolder).bind(item)
    }
}