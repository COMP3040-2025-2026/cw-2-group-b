package com.nottingham.mynottingham.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.ActivityMainBinding

/**
 * Main Activity - Entry point of the application
 * Handles bottom navigation and fragment navigation
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Top-level destinations (where back button should exit app)
    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.messageFragment,
        R.id.forumFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    /**
     * Setup Navigation Component with Bottom Navigation
     */
    private fun setupNavigation() {
        // Get NavController from NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Bottom Navigation with NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Hide/show bottom navigation based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    // Hide bottom navigation on login screen
                    binding.bottomNavigation.visibility = View.GONE
                }
                R.id.homeFragment,
                R.id.messageFragment,
                R.id.forumFragment,
                R.id.profileFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Handle back press
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (navController.currentDestination?.id in topLevelDestinations) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
