package com.carnot.fd.eol.features.login.data

import com.carnot.fd.eol.data.BaseResponse
import com.carnot.fd.eol.features.login.data.request.LoginAPIRequest
import com.carnot.fd.eol.features.login.data.response.OTPResponse
import com.carnot.fd.eol.features.login.domain.AuthRemoteDataSource
import com.carnot.fd.eol.network.ApiService
import retrofit2.Response

class AuthRemoteDataSourceImp(
    private val apiCalls: ApiService
) : AuthRemoteDataSource {

    override suspend fun loginVerifyAPICall(logInApiIRequest: LoginAPIRequest): Response<BaseResponse<OTPResponse>> {
        return apiCalls.loginVerifyAPICall(logInApiIRequest)
    }
}
