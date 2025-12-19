package com.carnot.fd.eol.network

import android.app.Application
import com.carnot.fd.eol.BuildConfig
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.JsonObject
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for Vehicle EOL Login against the external Mahindra API.
 *
 * This mirrors the "Vehicle EOL Login" request from the provided Postman collection.
 */
interface VehicleEolLoginService {

    @POST("/api/v2/login")
    suspend fun vehicleEolLogin(
        @Body body: JsonObject,
    ): Response<JsonObject>

    companion object {
        // Uses the same BASE_URL as all other APIs
        fun create(application: Application): VehicleEolLoginService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val chuckerInterceptor = ChuckerInterceptor.Builder(application)
                .collector(ChuckerCollector(application, showNotification = true))
                .maxContentLength(250000L)
                .redactHeaders(emptySet())
                .alwaysReadResponseBody(true)
                .build()

            // 🔹 Start from authorized client builder
            val baseClient = ApiService.createAuthorizedClient(application)

            val client = baseClient.newBuilder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chuckerInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(VehicleEolLoginService::class.java)
        }
    }
}



