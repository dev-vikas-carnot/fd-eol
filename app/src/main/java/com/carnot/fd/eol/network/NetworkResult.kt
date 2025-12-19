package com.carnot.fd.eol.network

sealed class NetworkResult<T>(
    val code: Int? = null,
    val data: T? = null,
    val message: String? = null
) {
    class Loading<T>(data: T? = null, message: String? = null) :
        NetworkResult<T>(data = data, message = message)

    class Success<T>(data: T, message: String? = null) :
        NetworkResult<T>(data = data, message = message)

    class Error<T>(code: Int, message: String, data: T? = null) :
        NetworkResult<T>(code, data, message)
}
