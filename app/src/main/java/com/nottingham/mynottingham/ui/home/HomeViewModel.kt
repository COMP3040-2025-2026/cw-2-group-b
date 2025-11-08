package com.nottingham.mynottingham.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for Home Fragment
 */
class HomeViewModel : ViewModel() {

    private val _welcomeMessage = MutableLiveData<String>()
    val welcomeMessage: LiveData<String> = _welcomeMessage

    init {
        loadWelcomeMessage()
    }

    private fun loadWelcomeMessage() {
        _welcomeMessage.value = "Welcome to My Nottingham"
    }
}
