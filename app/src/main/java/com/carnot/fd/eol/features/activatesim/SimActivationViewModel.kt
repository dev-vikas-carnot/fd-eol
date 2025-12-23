package com.carnot.fd.eol.features.activatesim

import android.app.Application
import androidx.lifecycle.*
import com.carnot.fd.eol.data.VehicleMappingData
import com.carnot.fd.eol.data.VehicleMappingRequest
import com.carnot.fd.eol.features.vehicle_mapping.data.DeviceMappingData
import com.carnot.fd.eol.features.vehicle_mapping.data.DeviceMappingRequest
import com.carnot.fd.eol.network.ApiResponse
import com.carnot.fd.eol.utils.PreferenceUtil
import com.carnot.fd.eol.utils.apiCall
import com.carnot.fd.eol.utils.logMessage
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class SimActivationViewModel(
    applicationContext: Application
) : AndroidViewModel(applicationContext) {

    private val apiService = SimActivationApiService.create(applicationContext)

    private val _imei = MutableLiveData("")
    val imei: LiveData<String> get() = _imei

    private val _iccid = MutableLiveData("")
    val iccid: LiveData<String> get() = _iccid

    private val _isSubmitEnabled = MutableLiveData(false)
    val isSubmitEnabled: LiveData<Boolean> get() = _isSubmitEnabled

    private val _apiResponse = MutableLiveData<ApiResponse<JsonObject?>>()
    val apiResponse: LiveData<ApiResponse<JsonObject?>> get() = _apiResponse

    /**
     * Expected QR formats:
     * 1. "IMEI"
     * 2. "IMEI ICCID"
     */
    fun setDeviceQR(result: String) {
        Timber.e("Inside scan qr result : $result")
        try {
            val parts = result.trim().split("\\s+".toRegex())

            val parsedImei = parts.getOrNull(0).orEmpty()
            val parsedIccid = parts.getOrNull(1).orEmpty()

            _imei.value = parsedImei
            _iccid.value = parsedIccid
            Timber.e("Inside scan qr result imei : $parsedImei")
            Timber.e("Inside scan qr result iccid : $parsedIccid")

            _isSubmitEnabled.value =
                isImeiValid(parsedImei) && isIccidValid(parsedIccid)

        } catch (e: Exception) {
            _imei.value = ""
            _iccid.value = ""
            _isSubmitEnabled.value = false
            Timber.e("Inside scan qr result : ${e.message}")
        }
    }

    fun setDeviceQRFromScan(result: String) {
        Timber.d("QR Scan result: $result")

        val parts = result.trim().split("\\s+".toRegex())
        val parsedImei = parts.getOrNull(0).orEmpty()
        val parsedIccid = parts.getOrNull(1).orEmpty()

        _imei.value = parsedImei
        _iccid.value = parsedIccid

        _isSubmitEnabled.value =
            isImeiValid(parsedImei) && isIccidValid(parsedIccid)
    }

    fun setImeiManually(imei: String) {
        Timber.d("Inside setImeiManually: $imei")
        _imei.value = imei.trim()
        _iccid.value = ""   // manual entry has no ICCID
//        _isSubmitEnabled.value = isImeiValid(_imei.value)
        _isSubmitEnabled.value =
            isImeiValid(_imei.value) && isIccidValid(_iccid.value)
    }

    fun setIccidManually(value: String) {
        val iccid = value.trim()
        _iccid.value = iccid
        _isSubmitEnabled.value =
            isImeiValid(_imei.value) && isIccidValid(iccid)
    }

    private fun isImeiValid(imei: String?): Boolean {
        return !imei.isNullOrBlank()
                && imei.length == 15
                && imei.all { it.isDigit() }
    }

    private fun isIccidValid(iccid: String?): Boolean {
        // ICCID usually 19–20 digits (keep relaxed for now)
        return !iccid.isNullOrBlank()
                && iccid.length in 18..20
                && iccid.all { it.isDigit() }
    }

    fun onSubmitClick() {
        val currentImei = _imei.value.orEmpty()
        val currentIccid = _iccid.value.orEmpty()

        viewModelScope.launch {

            if (!isImeiValid(currentImei) || !isIccidValid(currentIccid)) {
                delay(100)
                _apiResponse.value = ApiResponse.Error("Invalid IMEI or ICCID")
                return@launch
            }

            getApplication<Application>().logMessage("SIM Activation Flow Started")
            _apiResponse.value = ApiResponse.Loading

            apiCall(
                execute = {
                    val now = System.currentTimeMillis() / 1000

                    /** 1️⃣ Device Mapping */
                    val mappingRequest = DeviceMappingRequest(
                        mappingData = listOf(
                            DeviceMappingData(
                                IMEI = currentImei,
                                ICCID = currentIccid,
                                devPlantID = PreferenceUtil.vehiclePlantId,
                                devEOLStatus = true,
                                deviceVariant = "LOGGER",
                                deviceVersion = "CCU3.0",
                                devEOLDateTime =  now,
                                currentSimBatchID = "1840200125",
                                deviceMappedDateTime =now
                            )
                        )
                    )

                    val mappingResponse = apiService.deviceMapping(mappingRequest)
                    val proceed = when {
//                        mappingResponse.isSuccessful &&
//                                mappingResponse.body()?.status == true -> {
//                            getApplication<Application>().logMessage("Device Mapping Success")
//                            true
//                        }
//
//                        mappingResponse.errorBody()?.string()
//                            ?.contains("duplicate key", true) == true ||
//                                mappingResponse.body()?.message
//                                    ?.contains("duplicate key", true) == true -> {
//                            getApplication<Application>().logMessage(
//                                "Duplicate mapping detected, proceeding"
//                            )
//                            true
//                        }
//
//                        else -> {
//                            val msg = mappingResponse.body()?.message
//                                ?: "Device mapping failed"
//                            _apiResponse.postValue(ApiResponse.Error(msg))
//                            return@apiCall
//                        }
                        // ✅ Any 2xx response is success (200 / 201 / 204)
                        mappingResponse.isSuccessful -> {
                            getApplication<Application>().logMessage("Device Mapping Success")
                            true
                        }

                        // ✅ Allow duplicate mapping to proceed
                        mappingResponse.errorBody()?.string()
                            ?.contains("duplicate", true) == true ||
                                mappingResponse.body()?.message
                                    ?.contains("duplicate", true) == true -> {
                            getApplication<Application>().logMessage(
                                "Duplicate device mapping detected, proceeding"
                            )
                            true
                        }

                        else -> {
                            val msg = mappingResponse.body()?.message
                                ?: mappingResponse.errorBody()?.string()
                                ?: "Device mapping failed"
                            _apiResponse.postValue(ApiResponse.Error(msg))
                            return@apiCall
                        }
                    }

                    /** 2️⃣ SIM Activation */
                    if (proceed) {
                        val request = SimActivationRequest(
                            typeOfRequest = "STATUS_CHANGE",
                            simStatusReqOption = "INDIVIDUAL",
                            newSIMStatus = "BOOT_STRAP",
                            mappingData = listOf(
                                SimActivationMappingData(
                                    IMEI = currentImei,
                                )
                            )
                        )

                        val response = apiService.activateSim(request)

                        if (response.isSuccessful) {
                            _apiResponse.postValue(
                                ApiResponse.Success(
                                    response.body(),
                                    "SIM activation request submitted successfully"
                                )
                            )
                        } else {
                            _apiResponse.postValue(
                                ApiResponse.Error(
                                    response.errorBody()?.string()
                                        ?: response.message()
                                )
                            )
                        }
                    }
                },
                onException = {
                    _apiResponse.postValue(ApiResponse.Error(it.message.orEmpty()))
                    Timber.e("SimActivation API Exception Error : ${it.message}")
                },
                onNoInternet = {
                    _apiResponse.postValue(ApiResponse.Error("No internet"))
                    Timber.e("SimActivation API No internet")
                }
            )
        }
    }
}
