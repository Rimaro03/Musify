package com.rimaro.musify.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.rimaro.musify.R
import com.rimaro.musify.domain.model.DeezerAlbum
import com.rimaro.musify.domain.model.DeezerArtist
import com.rimaro.musify.domain.model.DeezerTrack
import com.rimaro.musify.databinding.ItemSearchAlbumBinding
import com.rimaro.musify.databinding.ItemSearchArtistBinding
import com.rimaro.musify.databinding.ItemSearchTrackBinding
import com.rimaro.musify.ui.search.SearchResultItem.AlbumItem
import com.rimaro.musify.ui.search.SearchResultItem.ArtistItem
import com.rimaro.musify.ui.search.SearchResultItem.TrackItem

class SearchResultAdapter (
    private val onTrackClick: (DeezerTrack) -> Unit,
    private val onArtistClick: (DeezerArtist) -> Unit,
    private val onAlbumClick: (DeezerAlbum) -> Unit
): ListAdapter<SearchResultItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        const val TYPE_TRACK = 0
        const val TYPE_ARTIST = 1
        const val TYPE_ALBUM = 2

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchResultItem>() {
            override fun areItemsTheSame(old: SearchResultItem, new: SearchResultItem) =
                when (old) {
                    is TrackItem if new is TrackItem -> old.track.id == new.track.id
                    is ArtistItem if new is ArtistItem -> old.artist.id == new.artist.id
                    is AlbumItem if new is AlbumItem -> old.album.id == new.album.id
                    else -> false
                }

            override fun areContentsTheSame(old: SearchResultItem, new: SearchResultItem) =
                old == new
        }
    }

    class TrackViewHolder(private val binding: ItemSearchTrackBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(track: DeezerTrack, onTrackClick: (DeezerTrack) -> Unit) {
                binding.searchTrackName.text = track.title
                binding.searchTrackArtist.text = track.artist?.name ?: "Unknown Artist"
                Glide.with(binding.root)
                    .load(track.album?.coverMedium)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(binding.searchTrackThumbnail)
                binding.searchTrackClickable.setOnClickListener { onTrackClick(track) }
            }
    }

    class AlbumViewHolder(private val binding: ItemSearchAlbumBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(album: DeezerAlbum) {
                binding.searchAlbumName.text = album.title
                binding.searchAlbumArtist.text = album.artist?.name ?: "Unknown Artist"
                Glide.with(itemView.context)
                    .load(album.coverMedium)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(binding.searchAlbumThumbnail)
            }
    }

    class ArtistViewHolder(private val binding: ItemSearchArtistBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(artist: DeezerArtist) {
                binding.searchArtistName.text = artist.name
                Glide.with(itemView.context)
                    .load(artist.pictureMedium)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .transform(CircleCrop())
                    .into(binding.searchArtistThumbnail)
            }
    }

    override fun getItemViewType(position: Int) = when(getItem(position)) {
        is SearchResultItem.TrackItem  -> TYPE_TRACK
        is SearchResultItem.ArtistItem -> TYPE_ARTIST
        is SearchResultItem.AlbumItem  -> TYPE_ALBUM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TRACK  -> TrackViewHolder(ItemSearchTrackBinding.inflate(inflater, parent, false))
            TYPE_ARTIST -> ArtistViewHolder(ItemSearchArtistBinding.inflate(inflater, parent, false))
            TYPE_ALBUM  -> AlbumViewHolder(ItemSearchAlbumBinding.inflate(inflater, parent, false))
            else        -> throw IllegalArgumentException("Invalid view type")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchResultItem.TrackItem  -> (holder as TrackViewHolder).bind(item.track, onTrackClick)
            is SearchResultItem.ArtistItem -> (holder as ArtistViewHolder).bind(item.artist)
            is SearchResultItem.AlbumItem  -> (holder as AlbumViewHolder).bind(item.album)
        }
    }
}

