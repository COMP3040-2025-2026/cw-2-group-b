package com.nottingham.mynottingham.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)

    private val _welcomeMessage = MutableLiveData<String>()
    val welcomeMessage: LiveData<String> = _welcomeMessage

    private val _facultyYearMessage = MutableLiveData<String>()
    val facultyYearMessage: LiveData<String> = _facultyYearMessage

    private val _isTeacher = MutableLiveData<Boolean>(false)
    val isTeacher: LiveData<Boolean> = _isTeacher

    init {
        loadWelcomeMessage()
        loadUserRoleInfo()
    }

    private fun loadWelcomeMessage() {
        viewModelScope.launch {
            val fullName = tokenManager.getFullName().firstOrNull()
            _welcomeMessage.value = if (fullName != null) "Hi, $fullName" else "Welcome to My Nottingham"
        }
    }

    private fun loadUserRoleInfo() {
        viewModelScope.launch {
            val userType = tokenManager.getUserType().firstOrNull()
            val faculty = tokenManager.getFaculty().firstOrNull()
            val year = tokenManager.getYearOfStudy().firstOrNull()
            val department = tokenManager.getDepartment().firstOrNull()

            if (userType != null) {
                if (userType.equals("TEACHER", ignoreCase = true)) {
                    _isTeacher.value = true
                    _facultyYearMessage.value = department ?: ""
                } else {
                    _isTeacher.value = false
                    _facultyYearMessage.value = when {
                        faculty != null && year != null -> "$faculty, Year $year"
                        faculty != null -> faculty
                        year != null -> "Year $year"
                        else -> ""
                    }
                }
            } else {
                _isTeacher.value = (year == null && faculty != null)
                _facultyYearMessage.value = if (_isTeacher.value == true) {
                    department ?: ""
                } else {
                    when {
                        faculty != null && year != null -> "$faculty, Year $year"
                        faculty != null -> faculty
                        year != null -> "Year $year"
                        else -> ""
                    }
                }
            }
        }
    }
}
