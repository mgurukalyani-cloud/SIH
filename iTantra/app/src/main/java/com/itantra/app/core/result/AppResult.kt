package com.itantra.app.core.result

sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val exception: Throwable, val message: String = exception.localizedMessage ?: "Unknown error") : AppResult<Nothing>()
    object Loading : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}
