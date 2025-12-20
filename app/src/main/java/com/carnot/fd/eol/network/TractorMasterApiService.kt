package com.carnot.fd.eol.network

import android.content.Context
import com.carnot.fd.eol.BuildConfig
import com.carnot.fd.eol.features.vehicle_mapping.domain.TractorMasterApi
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TractorMasterApiService {

    fun create(context: Context): TractorMasterApi {

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        val chuckerInterceptor = ChuckerInterceptor.Builder(context)
            .collector(
                ChuckerCollector(
                    context,
                    showNotification = BuildConfig.DEBUG
                )
            )
            .maxContentLength(250_000L)
            .alwaysReadResponseBody(true)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(chuckerInterceptor)
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TractorMasterApi::class.java)
    }
}
