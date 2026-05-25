package com.thedavelopers.eventqr.core.api

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T, val message: String? = null) : NetworkResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
