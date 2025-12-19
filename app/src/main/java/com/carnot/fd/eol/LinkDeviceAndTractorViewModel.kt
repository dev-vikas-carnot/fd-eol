package com.carnot.fd.eol

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.carnot.fd.eol.data.CreateDeviceRequest
import com.carnot.fd.eol.data.CreateDeviceResponse
import com.carnot.fd.eol.firebase.AnalyticsEvents.API_SD_DEVICE_CREATION
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_IMEI_ICCID_SCAN_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_LINK_DEVICE_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_QR_PARSE_ERROR
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_QR_VALIDATION_FAILED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_VIN_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_Step1_LDT
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_Step2_LDT
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_Submit_LDT
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_API
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_BACKEND
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_CLICK
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_LINKDEVICETRACTOR
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents.logCrashError
import com.carnot.fd.eol.network.ApiResponse
import com.carnot.fd.eol.network.ApiService
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.apiCall
import com.carnot.fd.eol.utils.logMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LinkDeviceAndTractorViewModel(val applicationContext: Application) : AndroidViewModel(applicationContext) {

    private val apiService: ApiService = ApiService.create(applicationContext)

    private val _isDeviceQrScanned = MutableLiveData(false)
    val isDeviceQrScanned: LiveData<Boolean> get() = _isDeviceQrScanned

    private val _isTractorVinScanned = MutableLiveData(false)
    val isTractorVinScanned: LiveData<Boolean> get() = _isTractorVinScanned

    private val _vin = MutableLiveData("")
    val vin: LiveData<String> get() = _vin

    private val _deviceImei = MutableLiveData("")
    val deviceImei: LiveData<String> get() = _deviceImei

    private val _deviceIccid = MutableLiveData("")
    val deviceIccid: LiveData<String> get() = _deviceIccid

    private val _isSubmitEnabled = MutableLiveData(false)
    val isSubmitEnabled: LiveData<Boolean> get() = _isSubmitEnabled

   val apiResponse: LiveData<ApiResponse<CreateDeviceResponse?>> get() = _apiResponse
    private val _apiResponse  = MutableLiveData<ApiResponse<CreateDeviceResponse?>>()


    fun setDeviceQr(result: String) {
        try {
            if (result.split(" ").size > 2){

                val imei = result.split(" ")[0]
                val iccid = result.split(" ")[1]

                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_CLICK)
                    putString("type_scan", "scan_qr")
                    putString("imei", imei)
                    putString("iccid", iccid)
                }
                FirebaseAnalyticsEvents.logEvent(EVENT_IMEI_ICCID_SCAN_SUCCESS,SCREEN_LINKDEVICETRACTOR,bundle)


                if (imei.length != 15) {
                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_CLICK)
                        putString("type_scan", "scan_qr_imei")
                        putString("imei", imei)
                        putString("iccid", iccid)
                        putString("error_message", "Wrong QR, Please provide Correct QR Code")

                    }
                FirebaseAnalyticsEvents.logEvent(EVENT_QR_VALIDATION_FAILED, SCREEN_LINKDEVICETRACTOR, bundle)

                    viewModelScope.launch {
                        delay(100)
                        _apiResponse.value = ApiResponse.Error("Wrong QR, Please provide Correct QR Code")
                    }


                    return;
                }

                if (iccid.length < 18 || iccid.length > 20) {

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_CLICK)
                        putString("type_scan", "scan_qr_iccid")
                        putString("imei", imei)
                        putString("iccid", iccid)
                        putString("error_message", "Wrong QR, Please provide Correct QR Code")

                    }
                FirebaseAnalyticsEvents.logEvent(EVENT_QR_VALIDATION_FAILED, SCREEN_LINKDEVICETRACTOR, bundle)

                    viewModelScope.launch {
                        delay(100)
                        _apiResponse.value = ApiResponse.Error("Wrong QR, Please provide Correct QR Code")
                    }
                    return;
                }
                _isDeviceQrScanned.value = true
                _deviceImei.value = imei
                _deviceIccid.value = iccid

                val bundle1 = Bundle().apply {
                    putString("event_type", EVENT_TYPE_CLICK)
                    putString("type_scan", "scan_qr")
                    putString("imei", imei)
                    putString("iccid", iccid)

                }
                FirebaseAnalyticsEvents.logEvent(EVENT_IMEI_ICCID_SCAN_SUCCESS, SCREEN_LINKDEVICETRACTOR, bundle1)
                FirebaseAnalyticsEvents.logEvent(EVENT_Step1_LDT, SCREEN_LINKDEVICETRACTOR, bundle1)
                checkSubmitEnabled()
            }else{

                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_CLICK)
                    putString("type_scan", "scan_qr")
                    putString("result",result)
                    putString("error_message", "Wrong QR, Please provide Correct QR Code")

                }

            FirebaseAnalyticsEvents.logEvent(EVENT_QR_VALIDATION_FAILED, SCREEN_LINKDEVICETRACTOR, bundle)

                viewModelScope.launch {
                    delay(100)
                    _apiResponse.value = ApiResponse.Error("Wrong QR, Please provide Correct QR Code")
                }
            }
        } catch (e: Exception) {
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_BACKEND)
                putString("error_message", e.localizedMessage)
            }
            FirebaseAnalyticsEvents.logEvent(EVENT_QR_PARSE_ERROR, SCREEN_LINKDEVICETRACTOR, bundle)

            e.printStackTrace()

            logCrashError(
                apiName = "EVENT_QR_PARSE_ERROR",
                error = e,
                message = "EVENT_QR_VALIDATION_FAILED"
            )
        }
    }

    fun setDeviceIccid(value: String){
        _deviceIccid.value =value
        checkSubmitEnabled()
    }

    fun setDeviceImei(value: String){
        _deviceImei.value =value
        checkSubmitEnabled()
    }

    fun setTractorVin(result: String) {
        if(Globals.isVinValidString(result)){
            _vin.value = result
            _isTractorVinScanned.value = true
            val bundle = Bundle().apply {
                putString("type_scan", "scan_vin")
                putString("event_type", EVENT_TYPE_CLICK)
                putString("vin", result)

            }
            FirebaseAnalyticsEvents.logEvent( EVENT_SCAN_VIN_SUCCESS, SCREEN_LINKDEVICETRACTOR,bundle)
            FirebaseAnalyticsEvents.logEvent(EVENT_Step2_LDT, SCREEN_LINKDEVICETRACTOR, bundle)

            checkSubmitEnabled()
        }else{
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
                putString("type_scan", "scan_vin")
                putString("result",result)
                putString("error_message", "Not a valid Vin")

            }
            FirebaseAnalyticsEvents.logEvent( EVENT_QR_VALIDATION_FAILED, SCREEN_LINKDEVICETRACTOR,bundle)

            viewModelScope.launch {
                delay(100)
                _apiResponse.value = ApiResponse.Error("Not a valid Vin")
            }
        }

        checkSubmitEnabled()
    }

    private fun checkSubmitEnabled() {
        _isSubmitEnabled.value = (_isDeviceQrScanned.value == true && _isTractorVinScanned.value == true) ||(deviceIccid.value?.isNotEmpty() == true && deviceImei.value?.isNotEmpty() == true && vin.value?.isNotEmpty() == true)
    }

    fun onSubmitClick(){
        viewModelScope.launch{
            applicationContext.logMessage("Create Device Request Api Start")

            // Log API call start
          FirebaseAnalyticsEvents.logApiCall("createDevice")

            _apiResponse.value = ApiResponse.Loading

            apiCall(
                execute = {
               val request = CreateDeviceRequest(
                    imei = _deviceImei.value.toString(),
                    iccid = _deviceIccid.value.toString(),
                    vin = _vin.value.toString(),
                    tractor_model_id = null
                )

                val requestBundle = Bundle().apply {
                    putString("imei", request.imei)
                    putString("iccid", request.iccid)
                    putString("vin", request.vin)
                }
                FirebaseAnalyticsEvents.logApiCall(API_SD_DEVICE_CREATION, requestBundle)

                val response = apiService.createDevice(request)
                applicationContext.logMessage("Create Device Request API Response Received")

                if (response.status){
                    _apiResponse.postValue( ApiResponse.Success(response.data))
                    applicationContext.logMessage("Create Device Request Api Success")

                   val successBundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("status", "success")
                       putString("imei", request.imei)
                       putString("iccid", request.iccid)
                       putString("vin", request.vin)
                    }
                    FirebaseAnalyticsEvents.logApiResponse(API_SD_DEVICE_CREATION, "Success", successBundle)
                    FirebaseAnalyticsEvents.logEvent(EVENT_Submit_LDT, SCREEN_LINKDEVICETRACTOR, successBundle)

                }else{
                    val errorBundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("error_message", response.message)
                    }
                    FirebaseAnalyticsEvents.logEvent( EVENT_LINK_DEVICE_SUCCESS, SCREEN_LINKDEVICETRACTOR,errorBundle)

                    FirebaseAnalyticsEvents.logError(
                        API_SD_DEVICE_CREATION,
                        Exception("API Error"),
                        response.message
                    )

                    _apiResponse.postValue( ApiResponse.Error(response.message))
                    applicationContext.logMessage("Create Device Request Api ${response.message}")
                }
            },
            onException = {
                FirebaseAnalyticsEvents.logError(API_SD_DEVICE_CREATION, it, "Network failure")

                _apiResponse.postValue( ApiResponse.Error(it.message.toString()))
                applicationContext.logMessage("Create Device Request Api Exception:${it.message}")
            },
            onNoInternet = {
              FirebaseAnalyticsEvents.logError(
                    API_SD_DEVICE_CREATION,
                    Exception("No Internet"),
                    "Internet not available"
                )

                _apiResponse.postValue( ApiResponse.Error("No internet"))
                applicationContext.logMessage("Create Device Request Api no internet")
            }
            )
        }
    }
}
