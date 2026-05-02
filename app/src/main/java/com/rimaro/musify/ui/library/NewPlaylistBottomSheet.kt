package com.rimaro.musify.ui.library

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rimaro.musify.databinding.FragmentNewPlaylistBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.apache.commons.csv.CSVFormat

@AndroidEntryPoint
class NewPlaylistBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentNewPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewPlaylistViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewPlaylistBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.newPlayDismissBtn.setOnClickListener {
            dismiss()
        }
        binding.newPlayCreate.setOnClickListener {  }

        binding.newPlayImportSingle.setOnClickListener {
            filePickerLauncher.launch("*/*")

        }
        binding.newPlayImportMultiple.setOnClickListener {  }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.importState.collect {
                Log.d("NewPlaylist", "state update: $it")
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFromCsv(it) }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}