package com.carnot.fd.eol.features.login.data

import com.carnot.fd.eol.features.login.data.request.LoginAPIRequest
import com.carnot.fd.eol.features.login.data.request.VerifyOTPAPIRequest
import com.carnot.fd.eol.features.login.data.response.OTPResponse
import com.carnot.fd.eol.features.login.domain.AuthRemoteDataSource
import com.carnot.fd.eol.data.BaseResponse
import com.carnot.fd.eol.network.ApiService
import com.google.gson.JsonObject
import retrofit2.Response
class AuthRemoteDataSourceImp  constructor(
    private val apiCalls: ApiService
) : AuthRemoteDataSource {
    override suspend fun loginAPICall(loginAPIRequest: LoginAPIRequest): Response<BaseResponse<JsonObject>> {
        return apiCalls.loginAPICall(loginAPIRequest)
    }

    override suspend fun loginVerifyAPICall(verifyOTPAPIRequest: VerifyOTPAPIRequest): Response<BaseResponse<OTPResponse>> {
        return apiCalls.loginVerifyAPICall(verifyOTPAPIRequest)
    }
}
