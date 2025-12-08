package com.nottingham.mynottingham

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basic instrumented tests for the My Nottingham application.
 *
 * These tests run on an Android device and verify fundamental
 * application properties and context access.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private lateinit var appContext: Context

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * Test that the app context has the correct package name
     */
    @Test
    fun useAppContext() {
        assertEquals("com.nottingham.mynottingham", appContext.packageName)
    }

    /**
     * Test that app context is not null
     */
    @Test
    fun appContext_isNotNull() {
        assertNotNull(appContext)
    }

    /**
     * Test that package manager is accessible
     */
    @Test
    fun packageManager_isAccessible() {
        val packageManager = appContext.packageManager
        assertNotNull(packageManager)
    }

    /**
     * Test that application info can be retrieved
     */
    @Test
    fun applicationInfo_isAccessible() {
        val appInfo = appContext.applicationInfo
        assertNotNull(appInfo)
        assertEquals("com.nottingham.mynottingham", appInfo.packageName)
    }

    /**
     * Test that resources are accessible
     */
    @Test
    fun resources_areAccessible() {
        val resources = appContext.resources
        assertNotNull(resources)

        // Verify app name string resource exists
        val appName = resources.getString(R.string.app_name)
        assertNotNull(appName)
        assertTrue(appName.isNotEmpty())
    }

    /**
     * Test that cache directory is accessible
     */
    @Test
    fun cacheDir_isAccessible() {
        val cacheDir = appContext.cacheDir
        assertNotNull(cacheDir)
        assertTrue(cacheDir.exists() || cacheDir.mkdirs())
    }

    /**
     * Test that files directory is accessible
     */
    @Test
    fun filesDir_isAccessible() {
        val filesDir = appContext.filesDir
        assertNotNull(filesDir)
        assertTrue(filesDir.exists() || filesDir.mkdirs())
    }

    /**
     * Test that database path can be generated
     */
    @Test
    fun databasePath_canBeGenerated() {
        val dbPath = appContext.getDatabasePath("test_db")
        assertNotNull(dbPath)
        assertTrue(dbPath.path.contains("test_db"))
    }

    /**
     * Test that SharedPreferences can be accessed
     */
    @Test
    fun sharedPreferences_canBeAccessed() {
        val prefs = appContext.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        assertNotNull(prefs)

        // Test write and read
        val editor = prefs.edit()
        editor.putString("test_key", "test_value")
        editor.apply()

        val value = prefs.getString("test_key", null)
        assertEquals("test_value", value)

        // Cleanup
        editor.clear().apply()
    }

    /**
     * Test that system services are accessible
     */
    @Test
    fun systemServices_areAccessible() {
        // ConnectivityManager
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
        assertNotNull(connectivityManager)

        // NotificationManager
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE)
        assertNotNull(notificationManager)
    }
}
