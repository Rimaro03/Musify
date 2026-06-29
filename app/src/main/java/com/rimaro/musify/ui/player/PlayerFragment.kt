package com.rimaro.musify.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentPlayerBinding

class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupItemsMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setupItemsMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_player_fragment, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    TODO("Not yet implemented")
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }
}

