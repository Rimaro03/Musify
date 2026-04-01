package com.rimaro.musify.ui.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.rimaro.musify.R
import com.rimaro.musify.databinding.FragmentSigninBinding
import kotlinx.coroutines.launch


class SigninFragment : Fragment() {
    private var _binding: FragmentSigninBinding? = null
    private val binding get() = _binding!!

    private var auth: FirebaseAuth? = null

    private val viewmodel: SigninViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideMenu()
        observeUiState()

        binding.signinBtnSignup.setOnClickListener {
            findNavController().navigate(R.id.action_signinFragment_to_signupSplashFragment)
        }

        auth = Firebase.auth
        binding.signinBtn.setOnClickListener {
            val email = binding.signinEmailEt.text.toString()
            val password = binding.signinPasswordEt.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireActivity(), "Fill all the fields", Toast.LENGTH_SHORT)
                    .show()
            } else {
                viewmodel.signin(email, password)
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Clear the existing menu to hide all items
                menu.clear()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.uiState.collect { uiState ->
                    binding.signinLoading.isVisible = uiState is SigninUiState.Loading
                    binding.signinBtn.isEnabled = uiState !is SigninUiState.Loading

                    when (uiState) {
                        is SigninUiState.Success -> {
                            findNavController().navigate(R.id.action_signinFragment_to_homeFragment)
                        }
                        is SigninUiState.Error -> {
                            Toast.makeText(
                                context,
                                uiState.message,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}