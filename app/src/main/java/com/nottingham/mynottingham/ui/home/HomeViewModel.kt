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
        loadFacultyYearInfo()
    }

    private fun loadWelcomeMessage() {
        viewModelScope.launch {
            val fullName = tokenManager.getFullName().firstOrNull()
            _welcomeMessage.value = if (fullName != null) {
                "Hi, $fullName"
            } else {
                "Welcome to My Nottingham"
            }
        }
    }

    private fun loadFacultyYearInfo() {
        viewModelScope.launch {
            val faculty = tokenManager.getFaculty().firstOrNull()
            val year = tokenManager.getYearOfStudy().firstOrNull()

            val message = when {
                faculty != null && year != null -> "$faculty, Year $year"
                faculty != null -> faculty
                year != null -> "Year $year"
                else -> ""
            }

            _facultyYearMessage.value = message

            _isTeacher.value = (year == null && faculty != null)
        }
    }
}
