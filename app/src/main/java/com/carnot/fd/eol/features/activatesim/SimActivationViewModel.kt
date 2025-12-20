package com.carnot.fd.eol.features.activatesim

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.carnot.fd.eol.network.ApiResponse
import com.carnot.fd.eol.utils.apiCall
import com.carnot.fd.eol.utils.logMessage
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for SIM activation by IMEI.
 * Mirrors the patterns used in other features (e.g. LinkDeviceAndTractor, Reprint).
 */
class SimActivationViewModel(
    private val applicationContext: Application
) : AndroidViewModel(applicationContext) {

    private val apiService: SimActivationApiService =
        SimActivationApiService.create(applicationContext)

    private val _imei = MutableLiveData("")
    val imei: LiveData<String> get() = _imei

    private val _isSubmitEnabled = MutableLiveData(false)
    val isSubmitEnabled: LiveData<Boolean> get() = _isSubmitEnabled

    private val _apiResponse = MutableLiveData<ApiResponse<JsonObject?>>()
    val apiResponse: LiveData<ApiResponse<JsonObject?>> get() = _apiResponse

    fun setImei(value: String) {
        try {
            val imei = value
                .trim()
                .split("\\s+".toRegex())
                .firstOrNull()
                .orEmpty()

            _imei.value = imei
            _isSubmitEnabled.value = isImeiValid(imei)

        } catch (e: Exception) {
            e.printStackTrace()
            _imei.value = ""
            _isSubmitEnabled.value = false
        }
    }

    private fun isImeiValid(imei: String?): Boolean {
        if (imei.isNullOrBlank()) return false
        // Reuse simple validation pattern from LinkDeviceAndTractor: length == 15
        if (imei.length != 15) return false
        // Ensure all digits
        return imei.all { it.isDigit() }
    }

    fun onSubmitClick() {
        val currentImei = _imei.value ?: ""
        viewModelScope.launch {
            if (!isImeiValid(currentImei)) {
                // Small delay to mimic pattern used elsewhere for error propagation
                delay(100)
                _apiResponse.value = ApiResponse.Error("Not a valid IMEI")
                return@launch
            }

            applicationContext.logMessage("SimActivation API Start")
            _apiResponse.value = ApiResponse.Loading

            apiCall(
                execute = {
                    val request = SimActivationRequest(
                        typeOfRequest = "STATUS_CHANGE",
                        simStatusReqOption = "INDIVIDUAL",
                        newSIMStatus = "BOOT_STRAP",
                        mappingData = listOf(
                            SimActivationMappingData(
                                IMEI = currentImei
                            )
                        )
                    )

                    val response = apiService.activateSim(request)
                    applicationContext.logMessage("SimActivation API Response Received: ${response.code()}")

                    if (response.isSuccessful) {
                        _apiResponse.postValue(
                            ApiResponse.Success(
                                data = response.body(),
                                message = "SIM activation request submitted successfully"
                            )
                        )
                    } else {
                        _apiResponse.postValue(
                            ApiResponse.Error(
                                "SIM activation failed: ${
                                    response.errorBody()?.string() ?: response.message()
                                }"
                            )
                        )
                    }
                },
                onException = {
                    _apiResponse.postValue(ApiResponse.Error(it.message.orEmpty()))
                    applicationContext.logMessage("SimActivation API Exception Error : ${it.message}")
                },
                onNoInternet = {
                    _apiResponse.postValue(ApiResponse.Error("No internet"))
                    applicationContext.logMessage("SimActivation API No internet")
                }
            )
        }
    }
}


