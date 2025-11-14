package com.nottingham.mynottingham.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nottingham.mynottingham.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for Home Fragment
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val _welcomeMessage = MutableLiveData<String>()
    val welcomeMessage: LiveData<String> = _welcomeMessage

    init {
        loadWelcomeMessage()
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
}
