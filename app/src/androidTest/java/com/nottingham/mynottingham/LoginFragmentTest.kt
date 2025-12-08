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
 * UI tests for LoginFragment
 *
 * Tests the login screen UI elements using Activity scenario:
 * - Login form elements exist
 * - UI components are properly initialized
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class LoginFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    /**
     * Test that activity launches successfully
     */
    @Test
    fun activity_launchesSuccessfully() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
        }
    }

    /**
     * Test that logo view exists
     */
    @Test
    fun logo_exists() {
        activityRule.scenario.onActivity { activity ->
            val logoView = activity.findViewById<View>(R.id.ivLogo)
            assertNotNull("Logo view should exist", logoView)
        }
    }

    /**
     * Test that app name view exists
     */
    @Test
    fun appName_exists() {
        activityRule.scenario.onActivity { activity ->
            val appNameView = activity.findViewById<View>(R.id.tvAppName)
            assertNotNull("App name view should exist", appNameView)
        }
    }

    /**
     * Test that welcome text view exists
     */
    @Test
    fun welcomeText_exists() {
        activityRule.scenario.onActivity { activity ->
            val welcomeView = activity.findViewById<View>(R.id.tvWelcome)
            assertNotNull("Welcome text view should exist", welcomeView)
        }
    }

    /**
     * Test that username input field exists
     */
    @Test
    fun usernameField_exists() {
        activityRule.scenario.onActivity { activity ->
            val usernameField = activity.findViewById<View>(R.id.etUsername)
            assertNotNull("Username field should exist", usernameField)
        }
    }

    /**
     * Test that password input field exists
     */
    @Test
    fun passwordField_exists() {
        activityRule.scenario.onActivity { activity ->
            val passwordField = activity.findViewById<View>(R.id.etPassword)
            assertNotNull("Password field should exist", passwordField)
        }
    }

    /**
     * Test that login button exists
     */
    @Test
    fun loginButton_exists() {
        activityRule.scenario.onActivity { activity ->
            val loginButton = activity.findViewById<View>(R.id.btnLogin)
            assertNotNull("Login button should exist", loginButton)
        }
    }

    /**
     * Test that login button is clickable
     */
    @Test
    fun loginButton_isClickable() {
        activityRule.scenario.onActivity { activity ->
            val loginButton = activity.findViewById<View>(R.id.btnLogin)
            assertNotNull("Login button should exist", loginButton)
            assertTrue("Login button should be clickable", loginButton.isClickable)
        }
    }

    /**
     * Test that error message is initially hidden
     */
    @Test
    fun errorMessage_initiallyHidden() {
        activityRule.scenario.onActivity { activity ->
            val errorView = activity.findViewById<View>(R.id.tvError)
            assertNotNull("Error view should exist", errorView)
            assertEquals("Error should be hidden", View.GONE, errorView.visibility)
        }
    }

    /**
     * Test that progress bar is initially hidden
     */
    @Test
    fun progressBar_initiallyHidden() {
        activityRule.scenario.onActivity { activity ->
            val progressBar = activity.findViewById<View>(R.id.progressBar)
            assertNotNull("Progress bar should exist", progressBar)
            assertEquals("Progress bar should be hidden", View.GONE, progressBar.visibility)
        }
    }

    /**
     * Test that form layout exists
     */
    @Test
    fun formLayout_exists() {
        activityRule.scenario.onActivity { activity ->
            val formLayout = activity.findViewById<View>(R.id.layoutForm)
            assertNotNull("Form layout should exist", formLayout)
        }
    }

    /**
     * Test that username TextInputLayout exists
     */
    @Test
    fun usernameInputLayout_exists() {
        activityRule.scenario.onActivity { activity ->
            val usernameLayout = activity.findViewById<View>(R.id.tilUsername)
            assertNotNull("Username input layout should exist", usernameLayout)
        }
    }

    /**
     * Test that password TextInputLayout exists
     */
    @Test
    fun passwordInputLayout_exists() {
        activityRule.scenario.onActivity { activity ->
            val passwordLayout = activity.findViewById<View>(R.id.tilPassword)
            assertNotNull("Password input layout should exist", passwordLayout)
        }
    }

    /**
     * Test that warning row exists
     */
    @Test
    fun warningRow_exists() {
        activityRule.scenario.onActivity { activity ->
            val warningRow = activity.findViewById<View>(R.id.warningRow)
            assertNotNull("Warning row should exist", warningRow)
        }
    }

    /**
     * Test that nav host fragment exists
     */
    @Test
    fun navHostFragment_exists() {
        activityRule.scenario.onActivity { activity ->
            val navHostFragment = activity.findViewById<View>(R.id.nav_host_fragment)
            assertNotNull("Nav host fragment should exist", navHostFragment)
        }
    }
}
