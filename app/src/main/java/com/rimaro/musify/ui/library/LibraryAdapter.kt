package com.rimaro.musify.ui.library

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.databinding.ItemLibraryPlaylistBinding
import com.rimaro.musify.domain.model.FirestorePlaylist
import java.io.File

class LibraryAdapter (
    private val onClick: (String) -> Unit
) : ListAdapter<FirestorePlaylist, RecyclerView.ViewHolder>(DIFF_CALLBACK)  {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FirestorePlaylist>() {
            override fun areItemsTheSame(oldItem: FirestorePlaylist, newItem: FirestorePlaylist): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: FirestorePlaylist, newItem: FirestorePlaylist): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class LibraryViewHolder(val binding: ItemLibraryPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: FirestorePlaylist, onClick: (String) -> Unit) {
            Glide.with(binding.root)
                .load(File(playlist.thumbnailPath))
                .centerCrop()
                .into(binding.libraryPlaylistCover)

            binding.libraryName.text = playlist.name
            binding.libraryPlaylistOrAlbum.text = "Playlist"
            val trackCount = this@LibraryAdapter.itemCount
            //binding.libraryTrackNum.text = "$trackCount tracks"
            binding.libraryPlayBtn.setOnClickListener {
                Log.d("play", "play func called with id ${playlist.id}")
                onClick(playlist.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val binding = ItemLibraryPlaylistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LibraryViewHolder(binding)
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        if (p0 is LibraryViewHolder) {
            p0.bind(getItem(p1), onClick)
        }
    }
}