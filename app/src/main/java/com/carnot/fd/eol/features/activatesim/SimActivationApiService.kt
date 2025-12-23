package com.carnot.fd.eol.features.activatesim

import android.app.Application
import com.carnot.fd.eol.BuildConfig
import com.carnot.fd.eol.data.BaseResponse
import com.carnot.fd.eol.data.VehicleMappingRequest
import com.carnot.fd.eol.features.vehicle_mapping.data.DeviceMappingRequest
import com.carnot.fd.eol.network.ApiService
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * SIM Activation API – uses the common EOL backend and centralized headers/JWT.
 */
interface SimActivationApiService {

    @PUT("/user-service/v1/vehiclemapping")
    suspend fun activateSim(
        @Body request: SimActivationRequest,
    ): Response<JsonObject>

    @POST("/user-service/v1/devicemapping")
    suspend fun deviceMapping(@Body request: DeviceMappingRequest): Response<BaseResponse<JsonObject>>

    companion object {
        fun create(application: Application): SimActivationApiService {
            val client = ApiService.createAuthorizedClient(application)

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(SimActivationApiService::class.java)
        }
    }
}
