package com.rimaro.musify.ui.common.playlistOptionsSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist
import com.rimaro.musify.databinding.FragmentPlaylistOptionsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistOptionsSheet : BottomSheetDialogFragment() {
    private var _binding: FragmentPlaylistOptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlaylistOptionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { playlist ->
                    playlist?.let { setupBottomSheet(it) }
                }
            }
        }
    }

    private fun setupBottomSheet(firestorePlaylist: FirestorePlaylist) {
        binding.playlistOptName.text = firestorePlaylist.name

        binding.playlistOptDelete.setOnClickListener {
            viewModel.deletePlaylist()
            findNavController().navigate(
                PlaylistOptionsSheetDirections.actionPlaylistOptionsToLibraryFragment()
            )
            dismiss()
        }

        binding.playlistOptDismissBtn.setOnClickListener { dismiss() }
    }
}