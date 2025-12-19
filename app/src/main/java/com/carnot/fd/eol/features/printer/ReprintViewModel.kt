package com.carnot.fd.eol.features.printer

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.carnot.fd.eol.data.DeviceStatusResponse
import com.carnot.fd.eol.features.printer.data.request.PostInstallationPrintRequest
import com.carnot.fd.eol.features.printer.data.response.PostInstallationPrintResponse
import com.carnot.fd.eol.firebase.AnalyticsEvents.API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCANSUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCAN_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_API
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_CLICK
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_REPRINT
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents
import com.carnot.fd.eol.network.ApiResponse
import com.carnot.fd.eol.network.ApiService
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.apiCall
import com.carnot.fd.eol.utils.logMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReprintViewModelViewModel(val applicationContext: Application): AndroidViewModel(applicationContext) {

    private val apiService: ApiService = ApiService.create(applicationContext)

    private val _isVinScanned = MutableLiveData(false)
    val isVinScanned: LiveData<Boolean> get() = _isVinScanned


    private val _isSubmitEnabled = MutableLiveData(false)
    val isSubmitEnabled: LiveData<Boolean> get() = _isSubmitEnabled


    private val _vin = MutableLiveData("")
    val vin: LiveData<String> get() = _vin

    val apiResponseStatus: LiveData<ApiResponse<DeviceStatusResponse?>> get() = _apiResponseStatus
    private val _apiResponseStatus  = MutableLiveData<ApiResponse<DeviceStatusResponse?>>()

    val apiResponseSubmit: LiveData<ApiResponse<PostInstallationPrintResponse?>> get() = _apiResponseSubmit
    private val _apiResponseSubmit  = MutableLiveData<ApiResponse<PostInstallationPrintResponse?>>()


    fun scanVin(result: String) {

        val bundle = Bundle().apply {
            putString("event_type", EVENT_TYPE_CLICK)
            putString("vin",result)
        }
        FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_SCANSUCCESS, SCREEN_REPRINT,bundle)

        if(Globals.isVinValidString(result)){
            // Simulate scanning VIN
            _isVinScanned.value = true
            _vin.value = result
            // Start verifying connectivity
            //verifyConnectivity()
            onVinScanned()
        }else{
            viewModelScope.launch {
                delay(100)
                _apiResponseStatus.value = ApiResponse.Error("Not a valid Vin")
            }
        }

    }

    fun setVin(vin:String){
        _vin.value = vin
    }

    fun submit(vinString: String) {
        viewModelScope.launch{

            val ipVin= vinString
            if(Globals.isVinValidString(ipVin)) {
                applicationContext.logMessage("Reprint Api Start")
                      // Log API call start in Firebase
            val initialBundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_API)
                putString("vin", ipVin)
            }
            FirebaseAnalyticsEvents.logApiCall(API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT, initialBundle)

                _isVinScanned.value = true
                _vin.value =vinString
                _apiResponseSubmit.value = ApiResponse.Loading

                apiCall(
                    execute = {
                     val request = PostInstallationPrintRequest(vin = ipVin)
                    val requestBundle = Bundle().apply {
                        putString("vin", request.vin)
                    }
                    FirebaseAnalyticsEvents.logApiCall(API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT, requestBundle)

                    val response = apiService.postInstallationPrint(request)
                    applicationContext.logMessage("Reprint Response Received")

                    if (response.status) {
                        FirebaseAnalyticsEvents.logApiResponse(API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT, "Success")

                        _apiResponseSubmit.postValue(ApiResponse.Success(response.data, response.message))
                        applicationContext.logMessage("Reprint Response Success")
                    } else {
                        val errorBundle = Bundle().apply {
                            putString("event_type", EVENT_TYPE_API)
                            putString("vin", ipVin)
                            putString("error_message", response.message)
                        }
                        FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_SCAN_SUCCESS, SCREEN_REPRINT, errorBundle)

                        FirebaseAnalyticsEvents.logError(
                            API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT,
                            Exception("API Error"),
                            response.status.toString()+"_"+response.message
                        )

                        _apiResponseSubmit.postValue(ApiResponse.Error(response.message))

                         applicationContext.logMessage("Reprint Response Error : ${response.message}")
                    }
                },
                onException = {
                    FirebaseAnalyticsEvents.logError(API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT, it, "Network failure")

                    _apiResponseSubmit.postValue(ApiResponse.Error(it.message.toString()))
                    applicationContext.logMessage("Reprint Api Exception Error : ${it.message}")
                },
                onNoInternet = {
                    FirebaseAnalyticsEvents.logError(
                        API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT,
                        Exception("No Internet"),
                        "Internet not available"
                    )

                    _apiResponseSubmit.postValue(ApiResponse.Error("No internet"))
                    applicationContext.logMessage("Reprint Api Exception No internet")
                }
                )
            } else{
                viewModelScope.launch {
                    delay(100)
                    _apiResponseStatus.value = ApiResponse.Error("Not a valid Vin")
                }
            }
        }
    }



    private fun onVinScanned(){

        _isSubmitEnabled.postValue(true)
    }
}