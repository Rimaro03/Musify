package com.rimaro.musify.ui.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.ItemPlaylistTrackBinding
import com.rimaro.musify.ui.common.model.TrackUiModel

class PlaylistTrackAdapter (
    private val onTrackClick: (TrackUiModel) -> Unit,
    private val onMenuClick: (TrackUiModel) -> Unit,
    private val onTrackLongClick: (TrackUiModel) -> Unit
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
            trackModel: TrackUiModel,
            onTrackClick: (TrackUiModel) -> Unit,
            onMenuClick: ((TrackUiModel) -> Unit),
            onTrackLongClick: (TrackUiModel) -> Unit) {
            val track = trackModel.track

            // track metadata
            binding.playlistTrackName.text = track.title
            binding.playlistTrackArtist.text = track.artist
            Glide.with(binding.root)
                .load(track.artworkUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.playlistTrackThumbnail)
            binding.playlistTrackClickable.setOnClickListener { onTrackClick(trackModel) }
            // dark shadow
            binding.loadingOverlay.visibility = if (track.streamUrl == null) {
                View.VISIBLE
            } else {
                View.GONE
            }
            // like btn
            binding.playlistTrackLikeBtn.isVisible = trackModel.isLiked == true
            // menu btn
            binding.playlistTrackMenuBtn.setOnClickListener {
                onMenuClick(trackModel)
            }
            binding.playlistTrackClickable.setOnLongClickListener {
                onTrackLongClick(trackModel)
                true
            }
            // highlight playing track
            binding.root.isActivated = trackModel.isPlaying
        }
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
        (holder as ViewHolder).bind(item, onTrackClick, onMenuClick, onTrackLongClick)
    }
}