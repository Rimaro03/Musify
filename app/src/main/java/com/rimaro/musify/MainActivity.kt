package com.rimaro.musify

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.rimaro.musify.databinding.ActivityMainBinding
import com.rimaro.musify.ui.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rimaro.musify.ui.player.PlayerViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val viewModel: SharedViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        setupAppbar(navController)

        setupBottomNav(navController)

        forceDarkMode()

        setupSearchbar()

        setupMiniplayer(navController)
        observePlayer()

        //hide menu on auth, login fragments
        navController.addOnDestinationChangedListener { _, _, _ ->
            invalidateOptionsMenu()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun setupBottomNav(navController: NavController) {
        val navView = binding.navView
        //navView.setupWithNavController(navController)
        navView.setOnItemSelectedListener { item ->
            val destinationId = when (item.itemId) {
                R.id.homeFragment -> R.id.homeFragment
                R.id.libraryFragment -> R.id.libraryFragment
                R.id.searchFragment -> R.id.searchFragment
                R.id.profileFragment -> R.id.profileFragment
                else -> null
            }

            destinationId?.let { dest ->
                val currentDest = navController.currentDestination?.id
                // navigating to search fragment twice automatically focus search bar
                if(currentDest == R.id.searchFragment && dest == R.id.searchFragment) {
                    viewModel.triggerSearchbar()
                } else {
                    if(currentDest != dest) {
                        navController.navigate(dest)
                    }
                }
            }
            true
        }

        // hide bottom nav on auth, login fragments
        val fragmentsNoNav = listOf(
            R.id.signinFragment,
            R.id.signupSplashFragment,
            R.id.signupEmailFragment,
            R.id.playerFragment
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id in fragmentsNoNav) {
                navView.visibility = View.GONE
            } else {
                navView.visibility = View.VISIBLE
            }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menuItem = when (destination.id) {
                R.id.homeFragment -> navView.menu.findItem(R.id.homeFragment)
                R.id.libraryFragment -> navView.menu.findItem(R.id.libraryFragment)
                R.id.searchFragment -> navView.menu.findItem(R.id.searchFragment)
                R.id.profileFragment -> navView.menu.findItem(R.id.profileFragment)
                else -> null
            }
            menuItem?.isChecked = true
        }
    }

    private fun setupAppbar(navController: NavController) {
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.signinFragment,
            R.id.signupSplashFragment,
            R.id.homeFragment,
            R.id.libraryFragment,
            R.id.searchFragment,
            R.id.profileFragment,
            R.id.playerFragment
        ))
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun forceDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    private fun setupSearchbar() {
        val searchBar = findViewById<SearchBar>(R.id.search_bar)
        val searchView = findViewById<SearchView>(R.id.search_view)
        searchView.setupWithSearchBar(searchBar) // only ever call this once
    }

    private fun setupMiniplayer(navController: NavController) {
        binding.miniplayer.apply {
            setOnPlayPauseClick {
                if(playerViewModel.isPlaying.value) {
                    playerViewModel.pause()
                } else {
                    playerViewModel.resume()
                }
            }

            setOnSkipClick { playerViewModel.skipNext() }

            setOnClick {
                findNavController(R.id.nav_host_fragment_content_main)
                    .navigate(R.id.action_global_playerFragment)
            }

            setOnSwipeLeft { playerViewModel.skipNext() }
            setOnSwipeRight { playerViewModel.skipPrevious() }
        }

        // hide/show miniplayer
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id == R.id.playerFragment) {
                binding.miniplayer.isVisible = false
            } else {
                val currentTrack = playerViewModel.currentTrack.value
                binding.miniplayer.isVisible = currentTrack != null
            }
        }
    }

    private fun observePlayer() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerViewModel.currentTrack.collect { track ->
                        val visible = track != null
                        binding.miniplayer.isVisible = visible
                        // Push nav host up when miniplayer appears
                    }
                }

                launch {
                    playerViewModel.currentTrack.collect { track ->
                        Log.d("MainActivity", "track: $track")
                        track?.let { binding.miniplayer.bind(it) }
                    }
                }

                launch {
                    playerViewModel.isPlaying.collect { state ->
                        binding.miniplayer.setPlaying(state)
                    }
                }
            }
        }
    }

}