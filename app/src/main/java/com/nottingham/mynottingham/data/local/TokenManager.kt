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
        private val EMAIL_KEY = stringPreferencesKey("email")
        private val AVATAR_KEY = stringPreferencesKey("avatar_url")
        private val PHONE_KEY = stringPreferencesKey("phone")

        // Student fields
        private val STUDENT_ID_KEY = stringPreferencesKey("student_id")
        private val FACULTY_KEY = stringPreferencesKey("faculty")
        private val MAJOR_KEY = stringPreferencesKey("major")
        private val YEAR_OF_STUDY_KEY = stringPreferencesKey("year_of_study")

        // Teacher fields
        private val EMPLOYEE_ID_KEY = stringPreferencesKey("employee_id")
        private val DEPARTMENT_KEY = stringPreferencesKey("department")
        private val TITLE_KEY = stringPreferencesKey("title")
        private val OFFICE_ROOM_KEY = stringPreferencesKey("office_room")
        private val OFFICE_HOURS_KEY = stringPreferencesKey("office_hours")
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

    suspend fun saveEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL_KEY] = email
        }
    }

    suspend fun saveAvatar(avatar: String) {
        context.dataStore.edit { it[AVATAR_KEY] = avatar }
    }

    suspend fun savePhone(phone: String) {
        context.dataStore.edit { it[PHONE_KEY] = phone }
    }

    suspend fun saveFaculty(faculty: String) {
        context.dataStore.edit { preferences ->
            preferences[FACULTY_KEY] = faculty
        }
    }

    suspend fun saveMajor(major: String) {
        context.dataStore.edit { preferences ->
            preferences[MAJOR_KEY] = major
        }
    }

    suspend fun saveYearOfStudy(yearOfStudy: String) {
        context.dataStore.edit { preferences ->
            preferences[YEAR_OF_STUDY_KEY] = yearOfStudy
        }
    }

    // Teacher save methods
    suspend fun saveEmployeeId(employeeId: String) {
        context.dataStore.edit { preferences ->
            preferences[EMPLOYEE_ID_KEY] = employeeId
        }
    }

    suspend fun saveDepartment(department: String) {
        context.dataStore.edit { preferences ->
            preferences[DEPARTMENT_KEY] = department
        }
    }

    suspend fun saveTitle(title: String) {
        context.dataStore.edit { preferences ->
            preferences[TITLE_KEY] = title
        }
    }

    suspend fun saveOfficeRoom(officeRoom: String) {
        context.dataStore.edit { preferences ->
            preferences[OFFICE_ROOM_KEY] = officeRoom
        }
    }

    suspend fun saveOfficeHours(officeHours: String) {
        context.dataStore.edit { preferences ->
            preferences[OFFICE_HOURS_KEY] = officeHours
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

    fun getEmail(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[EMAIL_KEY]
        }
    }

    fun getAvatar(): Flow<String?> {
        return context.dataStore.data.map { it[AVATAR_KEY] }
    }

    fun getPhone(): Flow<String?> {
        return context.dataStore.data.map { it[PHONE_KEY] }
    }

    fun getFaculty(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[FACULTY_KEY]
        }
    }

    fun getMajor(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[MAJOR_KEY]
        }
    }

    fun getYearOfStudy(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[YEAR_OF_STUDY_KEY]
        }
    }

    // Teacher get methods
    fun getEmployeeId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[EMPLOYEE_ID_KEY]
        }
    }

    fun getDepartment(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[DEPARTMENT_KEY]
        }
    }

    fun getTitle(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TITLE_KEY]
        }
    }

    fun getOfficeRoom(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[OFFICE_ROOM_KEY]
        }
    }

    fun getOfficeHours(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[OFFICE_HOURS_KEY]
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
