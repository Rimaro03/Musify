package com.rimaro.musify.ui.common.addToPlaylistSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rimaro.musify.databinding.FragmentAddToPlaylistBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddToPlaylistSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentAddToPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddToPlaylistViewModel by viewModels()

    private lateinit var playlistRv: RecyclerView
    private lateinit var playlistAdapter: AddToPlaylistAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistRv = binding.addToPlayRv
        playlistAdapter = AddToPlaylistAdapter({})
        playlistRv.adapter = playlistAdapter
        playlistRv.layoutManager = LinearLayoutManager(requireContext())
        setupSheet()
    }

    private fun setupSheet() {
        binding.addToPlayConfirmBtn.setOnClickListener {
            dismiss()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    when(uiState) {
                        is AddToPlaylistUiState.Success -> {
                            playlistAdapter.submitList(uiState.items)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}