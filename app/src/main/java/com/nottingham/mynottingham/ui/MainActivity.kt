package com.nottingham.mynottingham.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.data.local.TokenManager
import com.nottingham.mynottingham.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The main entry point of the application.
 * This Activity hosts the navigation graph and manages the visibility
 * and behavior of the bottom navigation bar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var tokenManager: TokenManager

    // Defines a set of top-level destinations. When the user is on these screens,
    // pressing the back button will exit the application instead of navigating up.
    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.messageFragment,
        R.id.forumFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding and set it as the content view.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the TokenManager for handling user authentication tokens.
        tokenManager = TokenManager(this)

        // Configure the navigation component.
        setupNavigation()
        // Check the user's login status to determine the initial screen.
        checkLoginStatus()
    }

    /**
     * Checks for an authentication token to determine if the user is already logged in.
     * If a token exists, it navigates to the home screen.
     */
    private fun checkLoginStatus() {
        lifecycleScope.launch {
            val token = tokenManager.getToken().first()
            if (!token.isNullOrEmpty()) {
                // User is considered logged in, navigate to the home fragment.
                navController.navigate(R.id.homeFragment)
            }
        }
    }

    /**
     * Sets up the Navigation Component, linking the NavController to the BottomNavigationView.
     * This method also manages the visibility of the bottom navigation bar, showing it only on top-level destinations.
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.itemIconTintList = null

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> binding.bottomNavigation.visibility = View.GONE
                R.id.homeFragment,
                R.id.messageFragment,
                R.id.forumFragment,
                R.id.profileFragment -> binding.bottomNavigation.visibility = View.VISIBLE
                else -> binding.bottomNavigation.visibility = View.GONE
            }
        }
    }

    /**
     * Overrides the system's back button behavior.
     * If the current screen is one of the top-level destinations, pressing back will close the app.
     * Otherwise, it performs the default "navigate up" action.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Check if the current destination is in our set of top-level destinations.
        if (navController.currentDestination?.id in topLevelDestinations) {
            // If so, exit the application.
            finish()
        } else {
            // Otherwise, let the system handle the back button press (i.e., navigate up).
            super.onBackPressed()
        }
    }
}
