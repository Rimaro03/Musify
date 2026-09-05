package com.rimaro.musify.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.rimaro.musify.R
import com.rimaro.musify.domain.model.DeezerAlbum
import com.rimaro.musify.domain.model.DeezerArtist
import com.rimaro.musify.databinding.ItemSearchAlbumBinding
import com.rimaro.musify.databinding.ItemSearchArtistBinding
import com.rimaro.musify.databinding.ItemSearchTrackBinding
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.ui.common.model.TrackUiModel
import com.rimaro.musify.ui.search.SearchResultItem.AlbumItem
import com.rimaro.musify.ui.search.SearchResultItem.ArtistItem
import com.rimaro.musify.ui.search.SearchResultItem.TrackItem

class SearchResultAdapter (
    private val onTrackClick: (TrackUiModel) -> Unit,
    private val onTrackLongClick: (TrackUiModel) -> Unit,
    private val onArtistClick: (DeezerArtist) -> Unit,
    private val onAlbumClick: (DeezerAlbum) -> Unit,
    private val onMenuClick: (TrackUiModel) -> Unit,
    private val onLikeBtnClick: (TrackUiModel) -> Unit
): ListAdapter<SearchResultItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        const val TYPE_TRACK = 0
        const val TYPE_ARTIST = 1
        const val TYPE_ALBUM = 2

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchResultItem>() {
            override fun areItemsTheSame(old: SearchResultItem, new: SearchResultItem) =
                when (old) {
                    is TrackItem if new is TrackItem -> old.trackModel.track.id == new.trackModel.track.id
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
            fun bind(trackModel: TrackUiModel,
                     onTrackClick: (TrackUiModel) -> Unit,
                     onTrackLongClick: (TrackUiModel) -> Unit,
                     onMenuClick: ((TrackUiModel) -> Unit),
                     onLikeBtnClick: (TrackUiModel) -> Unit
            ) {
                binding.root.setBackgroundResource(R.drawable.track_item_bg)
                val track = trackModel.track
                // track metadata
                binding.searchTrackName.text = track.title
                binding.searchTrackArtist.text = track.artist
                Glide.with(itemView.context)
                    .load(track.artworkUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(binding.searchTrackThumbnail)
                binding.searchTrackClickable.setOnClickListener {
                    onTrackClick(trackModel)
                }
                // dark shadow
                binding.loadingOverlay.visibility = if (track.streamUrl == null) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                // menu btn
                binding.searchTrackMenuBtn.setOnClickListener {
                    onMenuClick(trackModel)
                }
                binding.searchTrackClickable.setOnLongClickListener {
                    onTrackLongClick(trackModel)
                    true
                }
                // highlight playing track
                binding.root.isActivated = trackModel.isPlaying
                // liked track
                binding.searchTrackLikeBtn.isVisible = trackModel.isLiked == true
                binding.searchTrackLikeBtn.setOnClickListener {
                    onLikeBtnClick(trackModel)
                }
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
        is TrackItem  -> TYPE_TRACK
        is ArtistItem -> TYPE_ARTIST
        is AlbumItem  -> TYPE_ALBUM
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
            is TrackItem  -> (holder as TrackViewHolder).bind(item.trackModel, onTrackClick,
                onTrackLongClick, onMenuClick, onLikeBtnClick
            )
            is ArtistItem -> (holder as ArtistViewHolder).bind(item.artist)
            is AlbumItem  -> (holder as AlbumViewHolder).bind(item.album)
        }
    }
}

