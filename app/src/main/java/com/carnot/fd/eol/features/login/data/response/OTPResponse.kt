package com.carnot.fd.eol.features.login.data.response

import com.google.gson.annotations.SerializedName

data class OTPResponse(
    @SerializedName("user_id")
    val userId: Int,
    val name: String,
    @SerializedName("vehicle_plant_id")
    val vehiclePlantId: String?,
    @SerializedName("auth_tokens")
    val authTokens: AuthTokens
)

data class AuthTokens(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)
