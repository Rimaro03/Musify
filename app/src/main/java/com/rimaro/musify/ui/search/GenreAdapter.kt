package com.rimaro.musify.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rimaro.musify.databinding.ItemGenreBinding
import com.rimaro.musify.domain.dto.DeezerGenre

class GenreAdapter(
    private val onGenreClick: (DeezerGenre) -> Unit
) : ListAdapter<DeezerGenre, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeezerGenre>() {
            override fun areItemsTheSame(oldItem: DeezerGenre, newItem: DeezerGenre): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DeezerGenre, newItem: DeezerGenre): Boolean {
                return oldItem == newItem
            }
        }
    }

    class GenreViewHolder(val binding: ItemGenreBinding) :
        RecyclerView.ViewHolder(binding.root) {
            fun bind(genre: DeezerGenre, onGenreClick: (DeezerGenre) -> Unit) {
                Glide.with(binding.root)
                    .load(genre.pictureMedium)
                    .centerCrop()
                    .into(binding.genreImage)

                binding.genreName.text = genre.name
                binding.genreClickable.setOnClickListener { onGenreClick(genre) }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding = ItemGenreBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
        if (p0 is GenreViewHolder) {
            p0.bind(getItem(p1), onGenreClick)
        }
    }
}