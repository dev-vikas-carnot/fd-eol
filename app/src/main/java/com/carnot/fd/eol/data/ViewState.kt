package com.carnot.fd.eol.data

class ViewState<T>(val status: NetworkStatus, val data: T?, val message: String?) {
    enum class NetworkStatus {
        SUCCESS, ERROR, LOADING, INIT,MESSAGE
    }

    companion object {
        fun <T> success(data: T?, message: String?): ViewState<T?> {
            return ViewState(NetworkStatus.SUCCESS, data, message)
        }

        fun <T> error(data: T?, msg: String): ViewState<T?> {
            return ViewState(NetworkStatus.ERROR, data, msg)
        }

        fun <T> loading(data: T?, message: String?): ViewState<T?> {
            return ViewState(NetworkStatus.LOADING, data, message)
        }

        fun <T> message(data: T?, message: String?): ViewState<T?> {
            return ViewState(NetworkStatus.MESSAGE, data, message)
        }

        fun <T> init(): ViewState<T?> {
            return ViewState(NetworkStatus.INIT, null, null)
        }
    }
}