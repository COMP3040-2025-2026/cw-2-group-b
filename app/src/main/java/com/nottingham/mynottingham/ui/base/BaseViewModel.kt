package com.nottingham.mynottingham.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing common utilities for all ViewModels
 * Eliminates repetitive boilerplate code for:
 * - Loading state management
 * - Error handling
 * - Result processing
 */
abstract class BaseViewModel : ViewModel() {

    // Loading state
    protected val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    // Error state
    protected val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Execute a repository operation with automatic loading/error state handling
     *
     * @param T The expected result type
     * @param block Suspend lambda that returns Result<T>
     * @param onSuccess Lambda to execute on successful result
     * @param onError Optional lambda to execute on error (defaults to setting _error)
     */
    protected fun <T> launchDataLoad(
        block: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = block()
                result.onSuccess { data ->
                    onSuccess(data)
                }.onFailure { exception ->
                    if (onError != null) {
                        onError(exception as? Exception ?: Exception(exception.message))
                    } else {
                        _error.postValue(exception.message ?: "Unknown error occurred")
                    }
                }
            } catch (e: Exception) {
                if (onError != null) {
                    onError(e)
                } else {
                    _error.postValue(e.message ?: "Unknown error occurred")
                }
            } finally {
                _loading.postValue(false)
            }
        }
    }

    /**
     * Execute a repository operation without return data (e.g., delete, update)
     *
     * @param block Suspend lambda that returns Result<Unit>
     * @param onSuccess Lambda to execute on success
     * @param onError Optional lambda to execute on error
     */
    protected fun launchOperation(
        block: suspend () -> Result<Unit>,
        onSuccess: () -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        launchDataLoad(
            block = block,
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
