package com.nottingham.mynottingham

import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nottingham.mynottingham.ui.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for Navigation
 *
 * Tests the navigation functionality:
 * - Navigation host fragment
 * - Fragment navigation initialization
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class NavigationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Test that navigation host fragment exists
     */
    @Test
    fun navigationHost_exists() {
        activityRule.scenario.onActivity { activity ->
            val navHostFragment = activity.findViewById<View>(R.id.nav_host_fragment)
            assertNotNull("NavHostFragment should exist", navHostFragment)
        }
    }

    /**
     * Test navigation controller is properly initialized
     */
    @Test
    fun navController_isInitialized() {
        activityRule.scenario.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)
            assertNotNull("NavHostFragment should not be null", navHostFragment)
        }
    }

    /**
     * Test that the app starts with at least one fragment displayed
     */
    @Test
    fun app_startsWithFragment() {
        activityRule.scenario.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)
            assertNotNull("NavHostFragment should exist", navHostFragment)

            // Verify fragment manager has at least one fragment
            val childFragments = navHostFragment?.childFragmentManager?.fragments
            assertTrue(
                "Should have at least one fragment displayed",
                childFragments != null && childFragments.isNotEmpty()
            )
        }
    }

    /**
     * Test that nav host fragment is visible
     */
    @Test
    fun navHostFragment_isVisible() {
        activityRule.scenario.onActivity { activity ->
            val navHostFragment = activity.findViewById<View>(R.id.nav_host_fragment)
            assertNotNull("NavHostFragment should exist", navHostFragment)
            assertEquals("NavHostFragment should be visible", View.VISIBLE, navHostFragment.visibility)
        }
    }
}
