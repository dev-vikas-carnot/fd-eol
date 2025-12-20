package com.carnot.fd.eol.features.vehicle_mapping.domain

import android.content.Context
import com.carnot.fd.eol.data.BaseResponse
import com.carnot.fd.eol.network.TractorMasterApiService
import timber.log.Timber

class TractorMasterRepository(context: Context) {

    private val api = TractorMasterApiService.create(context)

//    suspend fun fetchIfNeeded() = api.getTractorModels(
//        mapOf("manufacturer" to "MAHINDRA")
//    )

    suspend fun fetchIfNeeded(): BaseResponse<List<TractorModel>> {
        // 1️⃣ Return from cache if available
        Timber.d("Inside fetchIfNeeded → cacheAvailable=${TractorMasterCache.isAvailable()}")
        if (TractorMasterCache.isAvailable()) {
            return BaseResponse(
                status = true,
                message = "Loaded from cache",
                data = TractorMasterCache.get()
            )
        }

        // 2️⃣ Otherwise call API (DO NOT save to cache here)
        return api.getTractorModels(
            mapOf("manufacturer" to "MAHINDRA")
        )
    }
}
