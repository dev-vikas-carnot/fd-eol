package com.carnot.fd.eol.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.carnot.fd.eol.utils.PreferenceUtil
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * General event tracking for user interactions, screen views, etc
 */
object FirebaseAnalyticsEvents {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun init(context: Context) {
        // ✅ Initialize PreferenceUtil before FirebaseAnalyticsEvents
        PreferenceUtil.initPreference(context)

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        PreferenceUtil.userName?.let {
            setUserProperty("user_name", it)
        }
    }

    fun logEvent(eventName: String, screenName: String? = null, bundle: Bundle? = null) {
        val eventBundle = bundle ?: Bundle()

        // ✅ Ensure PreferenceUtil is initialized before using userName
        PreferenceUtil.userName?.let {
            eventBundle.putString("user_name", it)
        }

        screenName?.let {
            eventBundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, it)
        }
        firebaseAnalytics.logEvent(eventName, eventBundle)
    }

    fun setUserProperty(propertyName: String, propertyValue: String) {
        firebaseAnalytics.setUserProperty(propertyName, propertyValue)
        Log.d("FirebaseAnalytics", "User property set: $propertyName = $propertyValue")
    }

    // Network-Specific Logging with Flexible Bundle Support
   fun logApiCall(apiName: String, bundle: Bundle? = null) {
    val logMessage = "API: $apiName | API Call Initiated"
    Log.v("AppAnalyticsLogger", logMessage)

    val eventBundle = bundle ?: Bundle().apply {
        putString(AnalyticsEvents.BUNDLE_KEY_NAME, apiName)
        putString(AnalyticsEvents.BUNDLE_KEY_PARAMS, bundle?.toString() ?: "No params provided")
    }

    firebaseAnalytics.logEvent(AnalyticsEvents.EVENT_NETWORK_APICALL, eventBundle)
    FirebaseCrashlytics.getInstance().log(logMessage)
}

fun logApiResponse(apiName: String, response: String, bundle: Bundle? = null) {
    val logMessage = "API: $apiName | Response: $response"
    Log.d("AppAnalyticsLogger", logMessage)

    val eventBundle = bundle ?: Bundle().apply {
        putString(AnalyticsEvents.BUNDLE_KEY_API_NAME, apiName)
        putString(AnalyticsEvents.BUNDLE_KEY_RESPONSE, response)
    }

    firebaseAnalytics.logEvent(AnalyticsEvents.EVENT_NETWORK_API_RESPONSE, eventBundle)
    FirebaseCrashlytics.getInstance().log(logMessage)
}

fun logError(apiName: String, error: Throwable, message: String, bundle: Bundle? = null) {
    val logMessage = "API: $apiName | Error: ${error.localizedMessage}"
    Log.e("AppAnalyticsLogger", logMessage, error)

    val eventBundle = bundle ?: Bundle().apply {
        putString(AnalyticsEvents.BUNDLE_KEY_NAME, apiName)
        putString(AnalyticsEvents.BUNDLE_KEY_ERROR, error.localizedMessage)
        putString(AnalyticsEvents.BUNDLE_KEY_MESSAGE, message)
    }

    firebaseAnalytics.logEvent(AnalyticsEvents.EVENT_NETWORK_ERROR, eventBundle)
    FirebaseCrashlytics.getInstance().recordException(error)
}

    fun logCrashError(apiName: String, error: Throwable, message: String) {
        val logMessage = "API: $apiName | Error: ${error.localizedMessage}"
        Log.e("AppAnalyticsLogger", logMessage, error)  // For local log visibility

        // Log the error details in Crashlytics only
        FirebaseCrashlytics.getInstance().apply {
            log("API Name: $apiName")
            log("Error Message: $message")
            log("Error Details: ${error.localizedMessage}")
            recordException(error)  // Captures the full stack trace in Crashlytics
        }
    }


}
