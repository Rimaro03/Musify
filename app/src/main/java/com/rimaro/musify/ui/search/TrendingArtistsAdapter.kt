package com.rimaro.musify.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.rimaro.musify.R
import com.rimaro.musify.databinding.ItemArtistBinding
import com.rimaro.musify.domain.dto.DeezerArtist

class TrendingArtistsAdapter(
    private val onArtistClick: (DeezerArtist) -> Unit,
): ListAdapter<DeezerArtist, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeezerArtist>() {
            override fun areItemsTheSame(old: DeezerArtist, new: DeezerArtist): Boolean {
                return old.id == new.id
            }

            override fun areContentsTheSame(old: DeezerArtist, new: DeezerArtist): Boolean {
                return old == new
            }
        }
    }

    class ArtistViewHolder(private val binding: ItemArtistBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(artist: DeezerArtist, onArtistClick: (DeezerArtist) -> Unit) {
                binding.itemArtistName.text = artist.name
                Glide.with(binding.root)
                    .load(artist.pictureMedium)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .transform(CircleCrop())
                    .into(binding.itemArtistPic)
                binding.itemArtistClickable.setOnClickListener { onArtistClick(artist) }
            }
        }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(p0.context)
        return ArtistViewHolder(ItemArtistBinding.inflate(inflater, p0, false))
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (holder is ArtistViewHolder) {
            holder.bind(getItem(position), onArtistClick)
        }
    }
}