package com.rimaro.musify.ui.library.playlists

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.ItemLibraryPlaylistBinding
import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist
import com.rimaro.musify.util.thumbnail.StorageManager
import java.io.File

class PlaylistsAdapter (
    private val onPlaylistClick: (String) -> Unit,
    private val createThumbnail: (String) -> Unit
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

    class LibraryViewHolder(val binding: ItemLibraryPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(playlist: FirestorePlaylist,
                 onPlaylistClick: (String) -> Unit,
                 createThumbnail: (String) -> Unit
        ) {
            val isPathValid = StorageManager.isFilePathValid(playlist.thumbnailPath)
            if(isPathValid) {
                Glide.with(binding.root)
                    .load(File(playlist.thumbnailPath))
                    .centerCrop()
                    .into(binding.libraryPlaylistCover)
            } else {
                if(playlist.tracks.count() > 3) {
                    createThumbnail(playlist.id)
                }
            }

            binding.libraryName.text = playlist.name
            binding.libraryPlaylistOrAlbum.text = "Playlist"
            //val trackCount = this@LibraryAdapter.itemCount
            //binding.libraryTrackNum.text = "$trackCount tracks"

            //binding.libraryPlayBtn.isEnabled = playerState != Player.STATE_BUFFERING
            binding.root.setOnClickListener { onPlaylistClick(playlist.id) }
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
            p0.bind(getItem(p1), onPlaylistClick, createThumbnail)
        }
    }
}