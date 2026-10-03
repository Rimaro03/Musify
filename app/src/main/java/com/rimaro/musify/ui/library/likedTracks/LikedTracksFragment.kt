package com.rimaro.musify.ui.library.likedTracks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.rimaro.musify.databinding.FragmentLikedTracksBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LikedTracksFragment : Fragment() {
    private var _binding: FragmentLikedTracksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LikedTracksViewModel by viewModels()

    private lateinit var likedTracksProgress: ProgressBar
    private lateinit var likedTracksRv: RecyclerView
    private lateinit var likedTracksCount: TextView
    private lateinit var likedTracksSearch: SearchBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLikedTracksBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        likedTracksProgress = binding.likedtracksProgress
        likedTracksRv = binding.likedtracksRv
        likedTracksCount = binding.likedtracksCount
        likedTracksSearch = binding.likedtracksSearchbar
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}