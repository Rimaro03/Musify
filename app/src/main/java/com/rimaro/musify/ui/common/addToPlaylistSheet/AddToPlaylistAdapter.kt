package com.rimaro.musify.ui.common.addToPlaylistSheet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.R
import com.rimaro.musify.databinding.ItemAddToPlaylistBinding
import com.rimaro.musify.domain.model.AddToPlaylistItem

class AddToPlaylistAdapter(
    private val toggleAddButton: (AddToPlaylistItem) -> Unit
) : ListAdapter<AddToPlaylistItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AddToPlaylistItem>() {
            override fun areItemsTheSame(old: AddToPlaylistItem, new: AddToPlaylistItem) =
                old.id == new.id

            override fun areContentsTheSame(old: AddToPlaylistItem, new: AddToPlaylistItem) =
                old == new
        }
    }

    class ViewHolder(private val binding: ItemAddToPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root){
            fun bind(
                playlistItem: AddToPlaylistItem,
                toggleAddButton: (AddToPlaylistItem) -> Unit
            ) {
                binding.addToPlayName.text = playlistItem.name
                Glide.with(binding.root)
                    .load(playlistItem.coverUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(binding.addToPlayThumbnail)
                binding.addToPlayBtn.icon = if(playlistItem.containsTrack) {
                    AppCompatResources.getDrawable(
                        binding.root.context,
                        androidx.media3.session.R.drawable.media3_icon_check_circle_filled
                    )
                } else {
                    AppCompatResources.getDrawable(
                        binding.root.context,
                        androidx.media3.session.R.drawable.media3_icon_plus_circle_unfilled
                    )
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemAddToPlaylistBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as ViewHolder).bind(item, toggleAddButton)
    }
}