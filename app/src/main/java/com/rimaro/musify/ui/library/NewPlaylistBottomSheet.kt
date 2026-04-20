package com.rimaro.musify.ui.library

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rimaro.musify.databinding.FragmentNewPlaylistBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewPlaylistBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentNewPlaylistBinding? = null
    private val binding get() = _binding!!

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
            filePickerLauncher.launch("text/csv")

        }
        binding.newPlayImportMultiple.setOnClickListener {  }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processCSV(it) }
    }

    private fun processCSV(uri: Uri) {
        context?.contentResolver?.openInputStream(uri)?.use { stream ->

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}