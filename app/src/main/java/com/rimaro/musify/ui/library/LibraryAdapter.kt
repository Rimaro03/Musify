package com.rimaro.musify.ui.library

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.ItemLibraryPlaylistBinding
import com.rimaro.musify.domain.model.FirestorePlaylist
import java.io.File

class LibraryAdapter (
    private val onClick: (String) -> Unit
) : ListAdapter<FirestorePlaylist, RecyclerView.ViewHolder>(DIFF_CALLBACK)  {
    private var fetchingTracks = false

    fun setPlayerLoading(isFetching: Boolean, playlistId: String) = run {
        fetchingTracks = isFetching
        val item = currentList.find { it.id == playlistId }
        item?.let {
            val position = currentList.indexOf(item)
            notifyItemChanged(position)
        }
    }

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

        private val progressDrawable = CircularProgressDrawable(binding.root.context).apply {
            setStyle(CircularProgressDrawable.DEFAULT)
            setColorSchemeColors(Color.BLACK)
            strokeWidth = 5f
            centerRadius = 20f
            start()
        }

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
            binding.libraryPlayBtn.icon = (
                if (fetchingTracks) {
                    progressDrawable
                } else {
                    ContextCompat.getDrawable(binding.root.context, R.drawable.play_arrow_24px)
                }
            )

            binding.libraryPlayBtn.isEnabled = !fetchingTracks
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