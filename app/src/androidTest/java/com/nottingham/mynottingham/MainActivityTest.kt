package com.nottingham.mynottingham

import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nottingham.mynottingham.ui.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for MainActivity
 *
 * Tests the main activity launch and basic UI elements:
 * - Activity launches successfully
 * - Navigation container exists
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Test that MainActivity launches successfully
     */
    @Test
    fun mainActivity_launches_successfully() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
        }
    }

    /**
     * Test that nav_host_fragment container exists
     */
    @Test
    fun navHostFragment_exists() {
        activityRule.scenario.onActivity { activity ->
            val navHostFragment = activity.findViewById<View>(R.id.nav_host_fragment)
            assertNotNull("NavHostFragment should exist", navHostFragment)
        }
    }

    /**
     * Test that the activity has content view
     */
    @Test
    fun activity_hasContentView() {
        activityRule.scenario.onActivity { activity ->
            val contentView = activity.findViewById<View>(android.R.id.content)
            assertNotNull("Content view should exist", contentView)
        }
    }

    /**
     * Test that activity window is not null
     */
    @Test
    fun activity_hasWindow() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Window should not be null", activity.window)
        }
    }

    /**
     * Test that activity has action bar or is in no action bar mode
     */
    @Test
    fun activity_actionBarConfiguration() {
        activityRule.scenario.onActivity { activity ->
            // Activity may or may not have action bar depending on theme
            // This just verifies the activity is properly configured
            val decorView = activity.window.decorView
            assertNotNull("Decor view should exist", decorView)
        }
    }
}
