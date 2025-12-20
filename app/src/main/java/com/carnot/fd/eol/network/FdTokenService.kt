package com.carnot.fd.eol.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.POST
import android.app.Application
import com.carnot.fd.eol.BuildConfig

/**
 * Used ONLY for refreshing FD EOL access token
 * using refresh_token received from fd/eol_login
 */
interface FdTokenService {

    @POST("/fd/token/refresh/")
    suspend fun refreshToken(
        @Header("Authorization") refreshToken: String
    ): Response<FdRefreshTokenResponse>

    companion object {
        fun create(application: Application): FdTokenService =
            Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FdTokenService::class.java)
    }
}

/* -------- Response Model -------- */

data class FdRefreshTokenResponse(
    @SerializedName("access_token")
    val accessToken: String
)
