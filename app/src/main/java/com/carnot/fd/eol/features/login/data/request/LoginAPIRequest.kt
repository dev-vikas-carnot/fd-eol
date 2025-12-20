package com.carnot.fd.eol.features.login.data.request

import com.google.gson.annotations.SerializedName

data class LoginAPIRequest(
    @SerializedName("user_name")
    val userName: String?,

    @SerializedName("otp")
    val otp: String? = null
)
