package com.rimaro.musify.ui.library.createPlaylist

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rimaro.musify.R
import com.rimaro.musify.databinding.DialogCreatePlaylistBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatePlaylistDialog : DialogFragment() {
    private val viewModel: CreatePlaylistViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCreatePlaylistBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_playlist)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.create, null) // overridden below
            .create()

        // Override the click so validation failures don't auto-dismiss the dialog
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = binding.nameInput.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    binding.nameLayout.error = getString(R.string.name_required)
                } else {
                    viewModel.createPlaylist(name)
                    dismiss()
                }
            }
        }
        return dialog
    }
}