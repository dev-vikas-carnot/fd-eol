package com.carnot.fd.eol.data

data class BaseResponse<T>(
    val status: Boolean,
    val message: String,
    val data: T?
)