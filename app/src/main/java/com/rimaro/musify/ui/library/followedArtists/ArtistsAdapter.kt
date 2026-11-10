package com.rimaro.musify.ui.library.followedArtists

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.data.remote.deezer.dto.DeezerArtist
import com.rimaro.musify.databinding.ItemArtistsBinding

class ArtistsAdapter (
    private val onArtistClick: (Long) -> Unit,
) : ListAdapter<DeezerArtist, RecyclerView.ViewHolder>(DIFF_CALLBACK)  {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeezerArtist>() {
            override fun areItemsTheSame(oldItem: DeezerArtist, newItem: DeezerArtist): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DeezerArtist, newItem: DeezerArtist): Boolean {
                return oldItem == newItem
            }
        }
    }

    class LibraryViewHolder(
        val binding: ItemArtistsBinding,
        private val onArtistClick: (Long) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: DeezerArtist) {
                Glide.with(binding.root)
                    .load(artist.pictureXl)
                    .centerCrop()
                    .into(binding.artistPicture)

            binding.artistName.text = artist.name

            binding.root.setOnClickListener { onArtistClick(artist.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val binding = ItemArtistsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LibraryViewHolder(binding, onArtistClick)
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        if (p0 is LibraryViewHolder) {
            p0.bind(getItem(p1))
        }
    }
}