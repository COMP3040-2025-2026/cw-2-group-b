package com.nottingham.mynottingham.ui.errand

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ErrandViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ErrandViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ErrandViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
