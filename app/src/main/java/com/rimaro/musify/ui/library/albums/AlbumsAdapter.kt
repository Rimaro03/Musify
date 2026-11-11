package com.rimaro.musify.ui.library.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.data.remote.deezer.dto.DeezerAlbum
import com.rimaro.musify.databinding.ItemAlbumBinding

class AlbumsAdapter (
    private val onAlbumClick: (Long) -> Unit,
) : ListAdapter<DeezerAlbum, RecyclerView.ViewHolder>(DIFF_CALLBACK)  {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeezerAlbum>() {
            override fun areItemsTheSame(oldItem: DeezerAlbum, newItem: DeezerAlbum): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DeezerAlbum, newItem: DeezerAlbum): Boolean {
                return oldItem == newItem
            }
        }
    }

    class AlbumViewHolder(
        val binding: ItemAlbumBinding,
        private val onAlbumClick: (Long) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(album: DeezerAlbum) {
                Glide.with(binding.root)
                    .load(album.coverXl)
                    .centerCrop()
                    .into(binding.albumPicture)

            binding.albumName.text = album.title

            binding.root.setOnClickListener { onAlbumClick(album.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlbumViewHolder(binding, onAlbumClick)
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        if (p0 is AlbumViewHolder) {
            p0.bind(getItem(p1))
        }
    }
}