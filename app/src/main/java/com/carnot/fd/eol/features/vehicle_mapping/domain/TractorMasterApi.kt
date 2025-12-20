package com.carnot.fd.eol.features.vehicle_mapping.domain

import com.carnot.fd.eol.data.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TractorMasterApi {

    @POST("/tractor/get_tractor_models_list/")
    suspend fun getTractorModels(
        @Body body: Map<String, String>
    ): BaseResponse<List<TractorModel>>
}
