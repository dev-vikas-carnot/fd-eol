package com.carnot.fd.eol.network

sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T?,val message: String? =null) : ApiResponse<T?>()
    data class Error(val message: String) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}