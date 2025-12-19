package com.carnot.fd.eol.features.login.data.request

import com.google.gson.annotations.SerializedName

data class VerifyOTPAPIRequest(
    @SerializedName("user_name")
    val userName: String?,
    val otp: String?
)
