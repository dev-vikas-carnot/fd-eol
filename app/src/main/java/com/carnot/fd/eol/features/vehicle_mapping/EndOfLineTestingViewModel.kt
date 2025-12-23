package com.carnot.fd.eol.features.vehicle_mapping

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.carnot.fd.eol.data.DeviceStatusRequest
import com.carnot.fd.eol.data.DeviceStatusResponse
import com.carnot.fd.eol.data.EolUiState
import com.carnot.fd.eol.data.VehicleMappingData
import com.carnot.fd.eol.data.VehicleMappingRequest
import com.carnot.fd.eol.firebase.AnalyticsEvents.API_SD_DEVICE_INSTALLATION_STATUS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_EOL_API_CALLED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_FLAGS_API_CALLED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_FLAGS_API_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_QR_VALIDATION_FAILED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_RETRY_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_VIN2_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_Step2_EOL
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_API
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_CLICK
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_EOL
import com.carnot.fd.eol.network.ApiResponse
import com.carnot.fd.eol.network.ApiService
import com.carnot.fd.eol.utils.Constants
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.PreferenceUtil
import com.carnot.fd.eol.utils.apiCall
import com.carnot.fd.eol.utils.logMessage
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class EndOfLineTestingViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    private val apiService: ApiService = ApiService.create(applicationContext)

    // === UI STATE MANAGEMENT ===
    private val _uiState = MutableLiveData<EolUiState>(EolUiState.ImeiInput)
    val uiState: LiveData<EolUiState> get() = _uiState

    // === DATA FIELDS ===
    private val _imei = MutableLiveData("")
    val imei: LiveData<String> get() = _imei

    private val _vin = MutableLiveData("")
    val vin: LiveData<String> get() = _vin

    // === DEVICE STATUS FLAGS ===
    private val _gpsLockStatus = MutableLiveData(false)
    val gpsLockStatus: LiveData<Boolean> get() = _gpsLockStatus

    private val _gsmPingStatus = MutableLiveData(false)
    val gsmPingStatus: LiveData<Boolean> get() = _gsmPingStatus

    private val _batteryChargingStatus = MutableLiveData(false)
    val batteryChargingStatus: LiveData<Boolean> get() = _batteryChargingStatus

    // === TIMER LOGIC (PRESERVED FROM EXISTING) ===
    private val _retryTime = MutableLiveData<String>("0")
    val retryTime: LiveData<String> get() = _retryTime

    val timerValue = 120
    var refreshCounter = 0
    var hideRefresh: MutableLiveData<Boolean> = MutableLiveData(false)
    var timeLeft = timerValue

    // Ensures we don't spawn overlapping 2-minute timers during recursive 2s polling.
    // (Fixes audit finding: timer was being restarted on each poll, breaking the 2-minute window.)
    private var retryTimerJob: Job? = null

    // === API RESPONSE STATES ===
    val apiResponseStatus: LiveData<ApiResponse<DeviceStatusResponse?>> get() = _apiResponseStatus
    private val _apiResponseStatus = MutableLiveData<ApiResponse<DeviceStatusResponse?>>()

    // TEMPORARY / TESTING ONLY: used when Constants.EOL_DUMMY_STATUS_ENABLED == true
    private var dummyStatusJob: Job? = null

    val apiResponseSubmit: LiveData<ApiResponse<JsonObject?>> get() = _apiResponseSubmit
    private val _apiResponseSubmit = MutableLiveData<ApiResponse<JsonObject?>>()

    var activationId = 0

    // --- Tractor Selection ---
    private var tractorSeries: String? = null
    private var tractorModel: String? = null

    fun setTractorSelection(series: String?, model: String?) {
        tractorSeries = series
        tractorModel = model
    }

    // === VALIDATION FUNCTIONS ===
    /**
     * IMEI validation: must be exactly 15 characters long
     */

    private fun isImeiValid(imei: String?): Boolean {
        return !imei.isNullOrBlank() &&
                imei.length == 15 &&
                imei.all { it.isDigit() }
    }

    /**
     * VIN validation using existing Globals utility
     * STRICTLY uses Globals.isVinValidString - NO OTHER VALIDATION
     */
    private fun isVinValid(vin: String?): Boolean {
        return !vin.isNullOrEmpty() && Globals.isVinValidString(vin)
    }

    // === IMEI SCANNING ===
    fun scanImei(qrResult: String) {
        // Only allow if in initial state
        Timber.e("Inside scanImei success :$qrResult }")
        if (_uiState.value != EolUiState.ImeiInput) return
        val imei = qrResult
            .trim()
            .split("\\s+".toRegex())
            .firstOrNull()
            .orEmpty()
        if (isImeiValid(imei)) {
            Timber.e("Inside scanImei success 1 :$imei }")
            _imei.value = imei
            _uiState.value = EolUiState.DeviceStatusPolling

            val successBundle = Bundle().apply {
                putString("type_scan", "scan_imei2")
                putString("event_type", EVENT_TYPE_CLICK)
                putString("imei", imei)
            }
//            // // FirebaseAnalyticsEvents.logEvent(EVENT_SCAN_VIN2_SUCCESS, SCREEN_EOL, successBundle)

            if (Constants.EOL_DUMMY_STATUS_ENABLED) {
                startDummyDeviceStatusFlow()
            } else {
                startDeviceStatusPolling(startNewWindow = true)
            }
        } else {
            Timber.e("Inside scanImei success 2 :$qrResult }")
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
                putString("type_scan", "scan_imei2")
                putString("result", qrResult)
                putString("error_message", "Not a valid IMEI")
            }
//            // FirebaseAnalyticsEvents.logEvent(EVENT_QR_VALIDATION_FAILED, SCREEN_EOL, bundle)

            viewModelScope.launch {
                delay(100)
                _apiResponseStatus.value = ApiResponse.Error("Not a valid IMEI")
            }
        }
    }

    // === VIN SCANNING / INPUT ===
    fun scanVin(result: String) {
        // Only allow VIN input if device status validation passed
        if (_uiState.value != EolUiState.VinInputEnabled && _uiState.value != EolUiState.SubmitReady) return

        setVin(result)
    }

    fun setVin(vinValue: String) {
        _vin.value = vinValue
        evaluateSubmitReadiness()
    }

    // === TIMER LOGIC (PRESERVED - UI CONTROLLED) ===
    private fun startRetryTimer() {
        // Cancel any previous timer to avoid multiple concurrent timers racing each other.
        retryTimerJob?.cancel()
        retryTimerJob = viewModelScope.launch {
            // 2 minute timer
            timeLeft = timerValue
            while (timeLeft > 0) {
                val minutes = timeLeft / 60
                val seconds = timeLeft % 60
                _retryTime.value = String.format("%02d:%02d", minutes, seconds)
                delay(1000)
                timeLeft -= 1
            }
            _retryTime.value = "0"
            if (refreshCounter == 1) {
                Log.d("EOL_Timer", "Retry window expired after 2 attempts")
                hideRefresh.value = true
            }
        }
    }

    // === SUBMIT READINESS EVALUATION ===
    /**
     * Check if all conditions are met to enable Submit button:
     * 1. All device flags must be PASS
     * 2. VIN must be present AND pass Globals.isVinValidString
     */
    private fun evaluateSubmitReadiness() {
        val allFlagsPassed = _gpsLockStatus.value == true &&
                _gsmPingStatus.value == true &&
                _batteryChargingStatus.value == true

        val vinIsValid = isVinValid(_vin.value)

        if (allFlagsPassed && vinIsValid) {
            _uiState.value = EolUiState.SubmitReady
        } else if (allFlagsPassed) {
            // Flags passed but VIN not valid yet
            _uiState.value = EolUiState.VinInputEnabled
        }
    }

    // === SUBMIT ACTION ===
    fun submit() {
        // Only allow submit if in SubmitReady state
        if (_uiState.value != EolUiState.SubmitReady) return

        // 🚨 Tractor selection validation
        if (tractorSeries.isNullOrBlank() || tractorModel.isNullOrBlank()) {
            _apiResponseSubmit.value =
                ApiResponse.Error("Please select Tractor Type and Model")
            return
        }

        // Final validation before submission
        if (!isVinValid(_vin.value)) {
            _apiResponseSubmit.value = ApiResponse.Error("Invalid VIN")
            return
        }

        _uiState.value = EolUiState.Submitted

        viewModelScope.launch {
            val now = System.currentTimeMillis() / 1000

            val vin = _vin.value.orEmpty()
            val imei = _imei.value.orEmpty()

            val analyticsBundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
                putString("vin", vin)
                putString("imei", imei)
                putString("vehicle_eol_status", "true")
            }

//            // FirebaseAnalyticsEvents.logEvent(
//                EVENT_EOL_API_CALLED,
//                SCREEN_EOL,
//                analyticsBundle
//            )

            _apiResponseSubmit.value = ApiResponse.Loading

            apiCall(
                execute = {
                    val request = VehicleMappingRequest(
                        mappingData = listOf(
                            VehicleMappingData(
                                vin = vin,
                                IMEI = imei,
                                vehicleType = tractorSeries ?: "UNKNOWN",
                                vehicleModel = tractorModel ?: "UNKNOWN",
                                vehiclePlantID = PreferenceUtil.vehiclePlantId,
                                vehicleCategory = "FD_DOM",
                                vehicleEOLStatus = true,
                                vehicleEOLDateTime = now,
                                vehicleSoldCountry = "IND",
                                vinParsingRequired = true,
                                vehicleMappedDateTime = now
                            )
                        )
                    )

                    val requestBundle = Bundle().apply {
                        putString("vin", vin)
                        putString("imei", imei)
                        putString("plant_id", Globals.getPlantKeyByIp(Globals.getPlantIp()))
                    }

                    val response = apiService.eolVehicleMapping(request)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val body = response.body()

                            if (body?.status == true) {
                                _apiResponseSubmit.value = ApiResponse.Success(
                                    body.data,
                                    body.message ?: "Vehicle mapping successful"
                                )
                                applicationContext.logMessage("EOL Vehicle Mapping Success")
                            } else {
                                _apiResponseSubmit.value = ApiResponse.Error(
                                    body?.message ?: "Vehicle mapping failed"
                                )
                            }

                        } else {
                            // 🔥 HTTP 400 / 422 / 500
                            val errorMsg = response.errorBody()
                                ?.string()
                                ?.let { parseErrorMessage(it) }
                                ?: "Invalid request"

                            _apiResponseSubmit.value = ApiResponse.Error(errorMsg)
                            applicationContext.logMessage("EOL Vehicle Mapping Failed: $errorMsg")
                        }
                    }
                },
                onException = {
                    _apiResponseSubmit.postValue(
                        ApiResponse.Error(it.message ?: "Something went wrong")
                    )
                },
                onNoInternet = {
                    _apiResponseSubmit.postValue(
                        ApiResponse.Error("No internet connection")
                    )
                }
            )
        }
    }

    private fun parseErrorMessage(errorJson: String): String {
        return try {
            val json = org.json.JSONObject(errorJson)
            json.optString("message", "Something went wrong")
        } catch (e: Exception) {
            "Something went wrong"
        }
    }

    // === RETRY LOGIC ===
    fun onRetry() {
        // TEMPORARY / TESTING ONLY: Retry must not appear in dummy mode.
        if (Constants.EOL_DUMMY_STATUS_ENABLED) return

        // Only allow retry during polling state
        if (_uiState.value != EolUiState.DeviceStatusPolling) return

        ++refreshCounter
        startRetryTimer()

        val bundle = Bundle().apply {
            putString("type_scan", "retry_device_status")
            putString("imei", _imei.value.toString())
            putString("event_type", EVENT_TYPE_CLICK)
        }
//        // FirebaseAnalyticsEvents.logEvent(EVENT_RETRY_CLICKED, SCREEN_EOL, bundle)

        // Restart polling (new 2-minute window)
        startDeviceStatusPolling(startNewWindow = true)
    }

    // === DUMMY DEVICE STATUS FLOW (TEMPORARY / TESTING ONLY) ===
    private fun startDummyDeviceStatusFlow() {
        // IMPORTANT: This must remain isolated and must not touch the real API-based polling logic.
        // Sequence: GPS -> GSM -> Battery (each becomes PASS after 4 seconds).

        // Hide retry UI in dummy mode.
        hideRefresh.postValue(true)

        // Stop any real timer/polling window (dummy does not use backend polling windows).
        retryTimerJob?.cancel()
        timeLeft = 0
        _retryTime.postValue("0")

        // Reset flags and begin simulated progression.
        _gpsLockStatus.postValue(false)
        _gsmPingStatus.postValue(false)
        _batteryChargingStatus.postValue(false)

        dummyStatusJob?.cancel()
        dummyStatusJob = viewModelScope.launch {
            // Keep state in DeviceStatusPolling until all flags PASS.
            _apiResponseStatus.postValue(ApiResponse.Loading)

            delay(4000)
            _gpsLockStatus.postValue(true)

            delay(4000)
            _gsmPingStatus.postValue(true)

            delay(4000)
            _batteryChargingStatus.postValue(true)

            // All flags PASS -> enable VIN.
            _uiState.postValue(EolUiState.VinInputEnabled)
            // Re-evaluate in case VIN was already provided (e.g., from previous attempt).
            evaluateSubmitReadiness()
        }
    }

    // === DEVICE STATUS POLLING (PRESERVED EXISTING LOGIC) ===
    private fun startDeviceStatusPolling(startNewWindow: Boolean) {
        // Start the 2-minute polling window ONCE per attempt (IMEI submit or Retry).
        // IMPORTANT: Do NOT restart the timer on each recursive 2-second poll.
        if (startNewWindow) {
            startRetryTimer()
        }

        val bundle = Bundle().apply {
            putString("type_scan", "device_status_polling")
            putString("imei", _imei.value.toString())
            putString("event_type", EVENT_TYPE_API)
        }
//        // FirebaseAnalyticsEvents.logEvent(EVENT_FLAGS_API_CALLED, SCREEN_EOL, bundle)



        viewModelScope.launch {
            applicationContext.logMessage("Get Device Status Api Start")

//            // FirebaseAnalyticsEvents.logApiCall(API_SD_DEVICE_INSTALLATION_STATUS)

            _apiResponseStatus.value = ApiResponse.Loading

            apiCall(
                execute = {
//                    val request = DeviceStatusRequest("860738072441508")
                    val request = DeviceStatusRequest(imei = _imei.value.toString())

                    val requestBundle = Bundle().apply {
                        putString("imei", request.imei)
                    }
//                    // FirebaseAnalyticsEvents.logApiCall(
//                        API_SD_DEVICE_INSTALLATION_STATUS,
//                        requestBundle
//                    )

                    val response = apiService.getDeviceStatus(request)
                    applicationContext.logMessage("Get Device Status Api Response Received")

                    withContext(Dispatchers.Main) {
                        if (response.status) {
//                            // FirebaseAnalyticsEvents.logApiResponse(
//                                API_SD_DEVICE_INSTALLATION_STATUS,
//                                "Success"
//                            )

                            _apiResponseStatus.value = ApiResponse.Success(response.data)
                            _gpsLockStatus.value = response.data!!.gps
                            _gsmPingStatus.value = response.data!!.gsm
                            _batteryChargingStatus.value = response.data!!.battery
                            activationId = response.data!!.activation_id

                            val successBundle = Bundle().apply {
                                putString("type_scan", "device_status_received")
                                putString("event_type", EVENT_TYPE_API)
                                putString("gps", response.data!!.gps.toString())
                                putString("gsm", response.data!!.gsm.toString())
                                putString("battery", response.data!!.battery.toString())
                                putString("activationId", response.data!!.activation_id.toString())
                            }
//                            // FirebaseAnalyticsEvents.logEvent(
//                                EVENT_FLAGS_API_SUCCESS,
//                                SCREEN_EOL,
//                                successBundle
//                            )
//                            // FirebaseAnalyticsEvents.logEvent(
//                                EVENT_Step2_EOL,
//                                SCREEN_EOL,
//                                successBundle
//                            )

                            // Check if all flags passed
                            if (_gpsLockStatus.value == true &&
                                _gsmPingStatus.value == true &&
                                _batteryChargingStatus.value == true
                            ) {
                                // ALL FLAGS PASSED - Enable VIN input
                                _uiState.value = EolUiState.VinInputEnabled
                            } else {
                                // Some flags still failing - continue polling if timer allows
                                if (timeLeft > 0) {
                                    delay(2000) // Poll every 2 seconds
                                    startDeviceStatusPolling(startNewWindow = false)
                                }
                            }

                            applicationContext.logMessage("Get Device Status Api Success")
                        } else {
                            // FirebaseAnalyticsEvents.logError(
//                                API_SD_DEVICE_INSTALLATION_STATUS,
//                                Exception("API Error"),
//                                response.message
//                            )

                            delay(1000)
                            // Reset to initial state on error
                            _uiState.postValue(EolUiState.ImeiInput)
                            _imei.postValue("")
                            _retryTime.postValue("0")
                            timeLeft = 0
                            refreshCounter = 0
                            _apiResponseStatus.value = ApiResponse.Error(response.message)
                            applicationContext.logMessage("Get Device Status Api Error : ${response.message}")
                        }
                    }
                },
                onException = {
//                    // FirebaseAnalyticsEvents.logError(
//                        API_SD_DEVICE_INSTALLATION_STATUS,
//                        it,
//                        "Network failure"
//                    )
                    _uiState.postValue(EolUiState.ImeiInput)
                    _imei.postValue("")
                    _retryTime.postValue("0")
                    timeLeft = 0
                    refreshCounter = 0
                    _apiResponseStatus.postValue(
                        ApiResponse.Error(
                            it.message ?: "Something Went Wrong"
                        )
                    )
                    applicationContext.logMessage("Get Device Status Api Exception : ${it.message}")
                },
                onNoInternet = {
                    // FirebaseAnalyticsEvents.logError(
//                        API_SD_DEVICE_INSTALLATION_STATUS,
//                        Exception("No Internet"),
//                        "Internet not available"
//                    )
                    _uiState.postValue(EolUiState.ImeiInput)
                    _imei.postValue("")
                    _retryTime.postValue("0")
                    timeLeft = 0
                    refreshCounter = 0
                    _apiResponseStatus.postValue(ApiResponse.Error("No internet"))
                    applicationContext.logMessage("Get Device Status Api: NO internet")
                }
            )
        }
    }
}
