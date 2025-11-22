package com.nottingham.mynottingham.data.repository

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
 * @param R The response wrapper type (e.g., ApiResponse<T>, ForumPostResponse, etc.)
 * @param T The expected data type from API response
 * @param apiCall Lambda to execute the Retrofit API call
 * @param extractData Lambda to extract data from response body
 * @param extractSuccess Lambda to check if response was successful
 * @param extractMessage Lambda to extract error message from response
 * @param saveFetchResult Optional lambda to save fetched data to local database
 * @return Result<T> containing either success data or failure exception
 */
suspend fun <R, T> networkBoundResource(
    apiCall: suspend () -> Response<R>,
    extractData: (R) -> T?,
    extractSuccess: (R) -> Boolean,
    extractMessage: (R) -> String?,
    saveFetchResult: (suspend (T) -> Unit)? = null
): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && extractSuccess(body)) {
                    val data = extractData(body)
                    if (data != null) {
                        // Save to local database if provided
                        saveFetchResult?.invoke(data)
                        Result.success(data)
                    } else {
                        Result.failure(Exception("Data is null"))
                    }
                } else {
                    val errorMessage = body?.let { extractMessage(it) }
                        ?: response.errorBody()?.string()
                        ?: "Request failed with code ${response.code()}"
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string()
                    ?: "Request failed with code ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Simplified variant for standard ApiResponse wrapper
 */
suspend fun <T> networkBoundResourceStandard(
    apiCall: suspend () -> Response<*>,
    extractData: (Any) -> T?,
    extractSuccess: (Any) -> Boolean,
    extractMessage: (Any) -> String?,
    saveFetchResult: (suspend (T) -> Unit)? = null
): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && extractSuccess(body)) {
                    val data = extractData(body)
                    if (data != null) {
                        saveFetchResult?.invoke(data)
                        Result.success(data)
                    } else {
                        Result.failure(Exception("Data is null"))
                    }
                } else {
                    val errorMessage = body?.let { extractMessage(it) }
                        ?: response.errorBody()?.string()
                        ?: "Request failed"
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Request failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Variant for API calls that don't return data (e.g., delete operations)
 */
suspend fun <R> networkBoundResourceNoData(
    apiCall: suspend () -> Response<R>,
    extractSuccess: (R) -> Boolean,
    extractMessage: (R) -> String?,
    onSuccess: (suspend () -> Unit)? = null
): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && extractSuccess(body)) {
                    onSuccess?.invoke()
                    Result.success(Unit)
                } else {
                    val errorMessage = body?.let { extractMessage(it) } ?: "Request failed"
                    Result.failure(Exception(errorMessage))
                }
            } else {
                Result.failure(Exception("Request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
