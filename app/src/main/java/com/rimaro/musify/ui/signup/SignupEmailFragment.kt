package com.rimaro.musify.ui.signup

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.lifecycle.Lifecycle
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.rimaro.musify.databinding.FragmentSignupEmailBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.rimaro.musify.R
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignupEmailFragment : Fragment() {
    private var _binding: FragmentSignupEmailBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val viewmodel: SignupViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideMenu()
        observeUiState()

        binding.signupPasswordEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCheckTextColor(s.toString())
            }
        })

        auth = Firebase.auth
        binding.signupCreateBtn.setOnClickListener {
            val email = binding.signupEmailEt.text.toString()
            val password = binding.signupPasswordEt.text.toString()
            val username = binding.signupNameEt.text.toString()

            if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
                Toast.makeText(requireActivity(), "Fill all the fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if(!checkPasswordStrength(password)) {
                Toast.makeText(requireActivity(), "Password too weak", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            viewmodel.signUp(username, email, password)
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
                    binding.signupLoading.isVisible = uiState is SignupUiState.Loading
                    binding.signupCreateBtn.isEnabled = uiState !is SignupUiState.Loading

                    when (uiState) {
                        is SignupUiState.Success -> {
                            findNavController().navigate(R.id.action_signupEmailFragment_to_homeFragment)
                        }
                        is SignupUiState.Error -> {
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

    /**
     * Update the color of the password check marks based on the password strength:
     * - Green if the password is valid
     * - Red if the password is not valid
     */
    private fun updateCheckTextColor(password: String) {
        val has10char = password.length >= 10
        val hasUpperLower = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
        val hasNumbers = password.any { it.isDigit() }
        val hasSymbols = password.any { !it.isLetterOrDigit() }

        binding.signupCheck10chr.setTextColor(
            if (has10char) resources.getColor(R.color.text_succ_green, null)
            else resources.getColor(R.color.text_err_red, null)
        )
        binding.signupCheckUpperlower.setTextColor(
            if (hasUpperLower) resources.getColor(R.color.text_succ_green, null)
            else resources.getColor(R.color.text_err_red, null)
        )
        binding.signupCheckNumbers.setTextColor(
            if (hasNumbers) resources.getColor(R.color.text_succ_green, null)
            else resources.getColor(R.color.text_err_red, null)
        )
        binding.signupCheckSymbols.setTextColor(
            if (hasSymbols) resources.getColor(R.color.text_succ_green, null)
            else resources.getColor(R.color.text_err_red, null)
        )
    }

    /**
     * Check if the password meets the requirements:
     * - Has at least 10 characters
     * - Has at least one uppercase letter
     * - Has at least one lowercase letter
     * - Has at least one number
     * - Has at least one symbol
     */
    private fun checkPasswordStrength(password: String): Boolean {
        val has10char = password.length >= 10
        val hasUpperLower = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
        val hasNumbers = password.any { it.isDigit() }
        val hasSymbols = password.any { !it.isLetterOrDigit() }

        return has10char && hasUpperLower && hasNumbers && hasSymbols
    }

    companion object {
        private const val TAG = "SignupEmailFragment"
    }
}