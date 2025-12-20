package com.carnot.fd.eol.features.login.domain

import com.carnot.fd.eol.data.BaseResponse
import com.carnot.fd.eol.features.login.data.request.LoginAPIRequest
import com.carnot.fd.eol.features.login.data.response.OTPResponse
import retrofit2.Response

interface AuthRemoteDataSource {

    suspend fun loginVerifyAPICall(logInApiIRequest: LoginAPIRequest):
            Response<BaseResponse<OTPResponse>>
}
