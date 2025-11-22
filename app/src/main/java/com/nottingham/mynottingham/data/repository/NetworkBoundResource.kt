package com.nottingham.mynottingham.data.repository

import com.nottingham.mynottingham.data.remote.dto.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Generic network request handler that eliminates boilerplate code
 * Automatically handles:
 * - IO thread switching
 * - Try-catch error handling
 * - Response validation
 * - Optional local caching
 *
 * @param T The expected data type from API response
 * @param apiCall Lambda to execute the Retrofit API call
 * @param saveFetchResult Optional lambda to save fetched data to local database
 * @return Result<T> containing either success data or failure exception
 */
suspend fun <T> networkBoundResource(
    apiCall: suspend () -> Response<ApiResponse<T>>,
    saveFetchResult: (suspend (T) -> Unit)? = null
): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    // Save to local database if provided
                    saveFetchResult?.invoke(data)
                    Result.success(data)
                } else {
                    Result.failure(Exception("Data is null"))
                }
            } else {
                val errorMessage = response.body()?.message
                    ?: response.errorBody()?.string()
                    ?: "Request failed with code ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Variant for API calls that don't return data (e.g., delete operations)
 *
 * @param apiCall Lambda to execute the Retrofit API call
 * @param onSuccess Optional lambda to execute on successful response
 * @return Result<Unit>
 */
suspend fun networkBoundResourceNoData(
    apiCall: suspend () -> Response<ApiResponse<*>>,
    onSuccess: (suspend () -> Unit)? = null
): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()

            if (response.isSuccessful && response.body()?.success == true) {
                onSuccess?.invoke()
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.message
                    ?: "Request failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
