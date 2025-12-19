package com.carnot.fd.eol.features.login.domain

import com.carnot.fd.eol.features.login.data.request.LoginAPIRequest
import com.carnot.fd.eol.features.login.data.request.VerifyOTPAPIRequest
import com.carnot.fd.eol.features.login.data.response.OTPResponse
import com.carnot.fd.eol.data.BaseResponse
import com.google.gson.JsonObject
import retrofit2.Response

interface AuthRemoteDataSource {

    suspend fun loginAPICall(loginAPIRequest: LoginAPIRequest):
        Response<BaseResponse<JsonObject>>

    suspend fun loginVerifyAPICall(verifyOTPAPIRequest: VerifyOTPAPIRequest):
        Response<BaseResponse<OTPResponse>>
}
