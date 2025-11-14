package com.nottingham.mynottingham.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val USER_TYPE_KEY = stringPreferencesKey("user_type")
        private val FULL_NAME_KEY = stringPreferencesKey("full_name")
        private val STUDENT_ID_KEY = stringPreferencesKey("student_id")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun saveUserInfo(userId: String, username: String, userType: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
            preferences[USER_TYPE_KEY] = userType
        }
    }

    suspend fun saveFullName(fullName: String) {
        context.dataStore.edit { preferences ->
            preferences[FULL_NAME_KEY] = fullName
        }
    }

    suspend fun saveStudentId(studentId: String) {
        context.dataStore.edit { preferences ->
            preferences[STUDENT_ID_KEY] = studentId
        }
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }

    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    }

    fun getUsername(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USERNAME_KEY]
        }
    }

    fun getUserType(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_TYPE_KEY]
        }
    }

    fun getFullName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[FULL_NAME_KEY]
        }
    }

    fun getStudentId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[STUDENT_ID_KEY]
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
