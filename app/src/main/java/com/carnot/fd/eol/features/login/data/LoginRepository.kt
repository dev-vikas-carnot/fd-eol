package com.carnot.fd.eol.features.login.data

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.carnot.fd.eol.features.login.data.request.LoginAPIRequest
import com.carnot.fd.eol.features.login.data.request.VerifyOTPAPIRequest
import com.carnot.fd.eol.features.login.data.response.OTPResponse
import com.carnot.fd.eol.features.login.domain.AuthRemoteDataSource
import com.carnot.fd.eol.firebase.AnalyticsEvents
import com.carnot.fd.eol.firebase.AnalyticsEvents.API_LOGIN_OTP
import com.carnot.fd.eol.firebase.AnalyticsEvents.API_LOGIN_VERIFY
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents
import com.carnot.fd.eol.network.ApiService
import com.carnot.fd.eol.network.NetworkResult
import com.google.gson.Gson
import com.google.gson.JsonObject
class LoginRepository( private val authRemoteDataSource: AuthRemoteDataSource)   {

    val loginState = MutableLiveData<NetworkResult<JsonObject>>()
    val otpState = MutableLiveData<NetworkResult<OTPResponse>>()

    suspend fun login(loginAPIRequest: LoginAPIRequest) {
        loginState.postValue(NetworkResult.Loading())

        FirebaseAnalyticsEvents.logApiCall("loginAPICall", Bundle().apply {
            putString("request_payload", Gson().toJson(loginAPIRequest))
        })

        try {
            val response = authRemoteDataSource.loginAPICall(loginAPIRequest)

            if (response.body()?.status == true) {
                FirebaseAnalyticsEvents.logApiResponse(
                    API_LOGIN_OTP,
                    response.body().toString()
                )
                loginState.postValue(NetworkResult.Success(response.body()?.data!!))
            } else {
                FirebaseAnalyticsEvents.logError(
                    API_LOGIN_OTP,
                    Exception("API Error"),
                    response.body()?.message ?: "Unknown error"
                )
                loginState.postValue(
                    NetworkResult.Error(
                        response.code(),
                        response.body()?.message!!
                    )
                )
            }
            }catch (e: Exception) {
            FirebaseAnalyticsEvents.logError(API_LOGIN_OTP, e, "Network failure")
            loginState.postValue(
                NetworkResult.Error(
                    -1,
                    e.localizedMessage ?: "Unknown error"
                )
            )


        }
    }

    suspend fun verifyOTP(verifyOTPAPIRequest: VerifyOTPAPIRequest) {
        otpState.postValue(NetworkResult.Loading())

        FirebaseAnalyticsEvents.logApiCall(API_LOGIN_VERIFY, Bundle().apply {
            putString("request_payload", Gson().toJson(verifyOTPAPIRequest))
        })

        try {
            val response = authRemoteDataSource.loginVerifyAPICall(verifyOTPAPIRequest)
            if (response.body()?.status == true) {
                FirebaseAnalyticsEvents.logApiResponse(
                    API_LOGIN_VERIFY,
                    response.body().toString()
                )
                otpState.postValue(NetworkResult.Success(response.body()?.data!!))
            } else {
                FirebaseAnalyticsEvents.logError(
                    API_LOGIN_VERIFY,
                    Exception("API Error"),
                    response.body()?.message ?: "Unknown error"
                )
                otpState.postValue(
                    NetworkResult.Error(
                        response.code(),
                        response.body()?.message!!
                    )
                )
            }
        } catch (e: Exception) {
            FirebaseAnalyticsEvents.logError(API_LOGIN_VERIFY, e, "Network failure")
            otpState.postValue(
                NetworkResult.Error(
                    -1,
                    e.localizedMessage ?: "Unknown error"
                )
            )
        }
    }
}
