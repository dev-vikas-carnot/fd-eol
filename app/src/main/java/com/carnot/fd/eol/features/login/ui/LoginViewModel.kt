package com.carnot.fd.eol.features.login.ui

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.*
import com.carnot.fd.eol.features.login.data.AuthRemoteDataSourceImp
import com.carnot.fd.eol.features.login.data.LoginRepository
import com.carnot.fd.eol.features.login.data.request.LoginAPIRequest
import com.carnot.fd.eol.network.ApiService
import com.carnot.fd.eol.network.NetworkResult
import com.carnot.fd.eol.utils.apiCall
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel constructor( var applicationContext: Application,

) :AndroidViewModel(applicationContext) {
    private val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
    }
    private var loginRepository: LoginRepository= LoginRepository(AuthRemoteDataSourceImp(ApiService.create(applicationContext)))

    private val _userName = MutableLiveData("")
    var userName = _userName.value
    private val _password = MutableLiveData("")
    var password = _password.value
    private var resendOtpCounter: CountDownTimer? = null
    private val timerResponse = MutableLiveData<NetworkResult<String>>()
    val getTimerResponse: LiveData<NetworkResult<String>> = timerResponse

    var loginState = loginRepository.loginState
    var otpState = loginRepository.otpState



    fun displayTimer(baseTime: Long) {
        resendOtpCounter = object : CountDownTimer(baseTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerResponse.value = NetworkResult.Loading("00:00", "timer")
                val timeInSec = millisUntilFinished / 1000
                if (timeInSec in 10..59) {
                    " 00:$timeInSec".also {
                        timerResponse.value = NetworkResult.Loading(it, "timer")
                    }
                } else if (timeInSec < 10) {
                    " 00:0$timeInSec".also {
                        timerResponse.value = NetworkResult.Loading(it, "timer")
                    }
                } else if (timeInSec >= 60) {
                    val minutes = timeInSec / 60
                    val seconds = timeInSec % 60
                    if (seconds >= 10) {
                        " 0$minutes:$seconds".also {
                            timerResponse.value = NetworkResult.Loading(it, "timer")
                        }
                    } else {
                        " 0$minutes:0$seconds".also {
                            timerResponse.value = NetworkResult.Loading(it, "timer")
                        }
                    }
                }
            }

            override fun onFinish() {
                timerResponse.value = NetworkResult.Success("", "success")
            }
        }.start()
    }

    private fun cancelTimer() {
        if (resendOtpCounter != null) resendOtpCounter?.cancel()
    }

    fun verifyLogin() {
        viewModelScope.launch(Dispatchers.IO + handler) {
            apiCall(execute = {
                val verifyOTPAPIRequest = LoginAPIRequest(
                    userName = userName,
                    otp = password
                )
                loginRepository.verifyOTP(verifyOTPAPIRequest)
            }, onNoInternet = {
                otpState.postValue(NetworkResult.Error(404,"No Internet Connection"))
            }, onException = {
                otpState.postValue(NetworkResult.Error(505,it.message.toString()))
            })

        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
    }
}
