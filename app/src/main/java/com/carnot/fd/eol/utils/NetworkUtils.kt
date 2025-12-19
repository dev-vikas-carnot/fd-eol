package com.carnot.fd.eol.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.telephony.TelephonyManager

object NetworkUtils {

    @kotlin.jvm.JvmStatic
    fun getNetworkType(applicationContext: Context): String {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                return if (networkInfo.type == ConnectivityManager.TYPE_WIFI) "WiFi" else {
                    getNetworkClass(applicationContext)
                }
            }
        } else {
            return "NOT CONNECTED"
        }
        return "NOT CONNECTED"
    }

    @SuppressLint("MissingPermission")
    private fun getNetworkClass(applicationContext: Context): String {
        val mTelephonyManager =
            applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        // checking networkType only if Android version 10 or lower because it doesn't require permission
        if (mTelephonyManager != null && Build.VERSION.SDK_INT < VERSION_CODES.R) {
            val networkType = mTelephonyManager.networkType
            return when (networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                else -> "Unknown"
            }
        }
        return "PD"
    }
}