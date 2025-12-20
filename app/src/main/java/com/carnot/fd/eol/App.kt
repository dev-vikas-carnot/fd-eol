package com.carnot.fd.eol

import android.app.Application
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree


@HiltAndroidApp
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Crashlytics (Required for Release builds)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Initialize Firebase Analytics (Ensures PreferenceUtil setup)
        FirebaseAnalyticsEvents.init(applicationContext)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())        }

    }

}