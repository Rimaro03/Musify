package com.rimaro.musify.ui.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.ItemPlaylistTrackBinding
import com.rimaro.musify.domain.model.Track

class PlaylistTrackAdapter (
    private val onTrackClick: (Track) -> Unit,
    private val onMenuClick: (Track) -> Unit,
    private val onTrackLongClick: (Track) -> Unit
): ListAdapter<Track, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Track>() {
            override fun areItemsTheSame(old: Track, new: Track) =
                old.id == new.id

            override fun areContentsTheSame(old: Track, new: Track) =
                old == new
        }
    }

    class ViewHolder(private val binding: ItemPlaylistTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            track: Track,
            onTrackClick: (Track) -> Unit,
            onMenuClick: ((Track) -> Unit),
            onTrackLongClick: (Track) -> Unit) {
            // track metadata
            binding.playlistTrackName.text = track.title
            binding.playlistTrackArtist.text = track.artist
            Glide.with(binding.root)
                .load(track.artworkUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.playlistTrackThumbnail)
            binding.playlistTrackClickable.setOnClickListener { onTrackClick(track) }
            // dark shadow
            binding.loadingOverlay.visibility = if (track.streamUrl == null) {
                View.VISIBLE
            } else {
                View.GONE
            }
            // menu btn
            binding.playlistTrackMenuBtn.setOnClickListener {
                onMenuClick(track)
            }
            binding.playlistTrackClickable.setOnLongClickListener {
                onTrackLongClick(track)
                true
            }
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