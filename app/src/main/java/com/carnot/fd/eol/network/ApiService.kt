package com.carnot.fd.eol.network

import android.app.Application
import android.os.Build
import com.carnot.fd.eol.BuildConfig
import com.carnot.fd.eol.data.*
import com.carnot.fd.eol.features.login.data.request.LoginAPIRequest
import com.carnot.fd.eol.features.login.data.response.OTPResponse
import com.carnot.fd.eol.features.printer.data.request.PostInstallationPrintRequest
import com.carnot.fd.eol.features.printer.data.response.PostInstallationPrintResponse
import com.carnot.fd.eol.utils.Constants
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.NetworkUtils
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("/fd/eol_login/")
    suspend fun loginVerifyAPICall(
        @Body request: LoginAPIRequest
    ): Response<BaseResponse<OTPResponse>>

    @POST("/amk/sd_device_creation/")
    suspend fun createDevice(@Body request: CreateDeviceRequest): BaseResponse<CreateDeviceResponse>

    @POST("/fd/device_installation_status/")
    suspend fun getDeviceStatus(@Body request: DeviceStatusRequest): BaseResponse<DeviceStatusResponse>

    @POST("/amk/sd_device_post_installation_test/")
    suspend fun postInstallationTest(@Body request: PostInstallationTestRequest): BaseResponse<PostInstallationTestResponse>

    @POST("/amk/sd_device_post_installation_status_print/")
    suspend fun postInstallationPrint(@Body request: PostInstallationPrintRequest): BaseResponse<PostInstallationPrintResponse>

    @POST("/user-service/v1/vehiclemapping")
    suspend fun eolVehicleMapping(@Body request: VehicleMappingRequest): retrofit2.Response<BaseResponse<JsonObject>>

    companion object {

        private const val RETRY_HEADER = "X-Auth-Retry"

        fun createAuthorizedClient(application: Application): OkHttpClient {

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            }

            val chuckerInterceptor = ChuckerInterceptor.Builder(application)
                .collector(ChuckerCollector(application, showNotification = BuildConfig.DEBUG))
                .maxContentLength(250_000L)
                .alwaysReadResponseBody(true)
                .build()

            return OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)

                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(chuckerInterceptor)
                        addInterceptor(loggingInterceptor)
                    }
                }

                /* ---------------- COMMON HEADERS ---------------- */
                .addInterceptor { chain ->
                    val original = chain.request()
                    val path = original.url.encodedPath
                    val token = Globals.getEolAccessToken(application)

                    // 🚨 SPECIAL CASE: device_installation_status
                    if (path.startsWith("/fd/device_installation_status/")) {

                        val specialRequest = original.newBuilder()
                            .headers(okhttp3.Headers.Builder().build()) // ❌ remove ALL headers
                            .header("Authorization", token ?: "")
                            .header(
                                "X-DreamFactory-API-Key",
                                Constants.DREAMFACTORY_API_KEY
                            )
                            .build()

                        return@addInterceptor chain.proceed(specialRequest)
                    }

                    // ✅ NORMAL FLOW FOR ALL OTHER APIs
                    val builder = original.newBuilder()
                        .header("app-build", BuildConfig.BUILD_TYPE)
                        .header("app-version", BuildConfig.VERSION_NAME)
                        .header("api-version", "v1")
                        .header("phone-network", NetworkUtils.getNetworkType(application))
                        .header("model", Build.MODEL)
                        .header("brand", Build.BRAND)
                        .header("manufacturer", Build.MANUFACTURER)
                        .header("android-version", Build.VERSION.RELEASE)
                        .header("app-package", BuildConfig.APPLICATION_ID)
                        .header("app-source", "sd-eol")
                        .header("platform", Constants.ANDROID)
                        .header("X-DreamFactory-API-Key", Constants.DREAMFACTORY_API_KEY)

                    if (!path.startsWith("/api/v2/login")) {
                        token?.let { builder.header("Authorization", it) }
                    }

                    chain.proceed(builder.build())
                }

                /* ---------------- TOKEN REFRESH ---------------- */
                .addInterceptor { chain ->
                    val request = chain.request()
                    val path = request.url.encodedPath
                    val alreadyRetried = request.header(RETRY_HEADER) == "true"

                    val response = chain.proceed(request)

                    if (
                        response.code != 401 ||
                        alreadyRetried ||
                        path.startsWith("/api/v2/login")
                    ) {
                        return@addInterceptor response
                    }

                    response.close()

                    val newToken = runBlocking {
                        refreshJwtToken(application)
                    }

                    if (newToken.isNullOrEmpty()) return@addInterceptor response

                    Globals.setEolAccessToken(application, newToken)

                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .header("Authorization", "$newToken")
                        .header(RETRY_HEADER, "true")
                        .build()

                    chain.proceed(newRequest)
                }
                .build()
        }

        /* ---------------- JWT REFRESH USING /api/v2/login ---------------- */
        private suspend fun refreshJwtToken(application: Application): String? {
            return try {
                val service = VehicleEolLoginService.create(application)
                val body = JsonObject().apply {
                    addProperty("userName", "TESTINGVEHICLEEOL@gmail.com")
                    addProperty("password", "Abc@123")
                    addProperty("requestedFrom", "WEB")
                    addProperty("vehicleCategory", "FD_DOM")
                    addProperty("operationCategory", "VEHICLE_EOL")
                }

                val response = service.vehicleEolLogin(body)
                if (!response.isSuccessful) null
                else response.body()?.get("token")?.asString
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }

        fun create(application: Application): ApiService =
            Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createAuthorizedClient(application))
                .build()
                .create(ApiService::class.java)
    }
}
