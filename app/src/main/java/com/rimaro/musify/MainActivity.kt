package com.rimaro.musify

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.rimaro.musify.databinding.ActivityMainBinding
import com.rimaro.musify.ui.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.size
import androidx.core.view.get

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val viewModel: SharedViewModel by viewModels()

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
            R.id.signupEmailFragment
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
            R.id.profileFragment
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
}