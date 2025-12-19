package com.carnot.fd.eol.features.login.data.response

import com.google.gson.annotations.SerializedName

data class OTPResponse(
    @SerializedName("user_id")
    val id: Int,
    val name: String,
    val phone: Long,
    @SerializedName("auth_tokens")
    val authTokens: AuthTokens
)

data class AuthTokens(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)
