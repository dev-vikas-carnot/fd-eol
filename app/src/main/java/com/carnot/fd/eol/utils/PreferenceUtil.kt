package com.carnot.fd.eol.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceUtil {

    lateinit var sharedPreferences: SharedPreferences

    fun initPreference(context: Context) {
        sharedPreferences = context.getSharedPreferences(
            Constants.PREFERENCE_COM_CARNOT_EOL_APP,
            Context.MODE_PRIVATE
        )
    }

    /**
     * SharedPreferences extension function, so we won't need to call edit() and apply()
     * ourselves on every SharedPreferences operation.
     */
    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    operator fun SharedPreferences.set(key: String, value: Any?) = when (value) {
        is String? -> edit { it.putString(key, value) }
        is Int -> edit { it.putInt(key, value) }
        is Boolean -> edit { it.putBoolean(key, value) }
        is Float -> edit { it.putFloat(key, value) }
        is Long -> edit { it.putLong(key, value) }
        else -> throw UnsupportedOperationException("Not yet implemented")
    }

    inline operator fun <reified T : Any> SharedPreferences.get(
        key: String,
        defaultValue: T? = null
    ): T = when (T::class) {
        String::class -> getString(key, defaultValue as? String ?: "") as T
        Int::class -> getInt(key, defaultValue as? Int ?: -1) as T
        Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T
        Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T
        Long::class -> getLong(key, defaultValue as? Long ?: -1) as T
        Set::class -> getStringSet(key, defaultValue as? MutableSet<String> ?: null) as T
        else -> throw UnsupportedOperationException("Not yet implemented")
    }

    var userId
        set(value) = sharedPreferences.set(Constants.PREFERENCE_USER_ID, value)
        get() = sharedPreferences[Constants.PREFERENCE_USER_ID, ""]

    var mobileNumber
        set(value) = sharedPreferences.set(Constants.PREFERENCE_MOBILE_NUMBER, value)
        get() = sharedPreferences[Constants.PREFERENCE_MOBILE_NUMBER, ""]


    var userName
        set(value) = sharedPreferences.set(Constants.PREFERENCE_USER_NAME, value)
        get() = sharedPreferences[Constants.PREFERENCE_USER_NAME, ""]

    var vehiclePlantId
        set(value) = sharedPreferences.set(Constants.PREFERENCE_PLANT_ID, value)
        get() = sharedPreferences[Constants.PREFERENCE_PLANT_ID, ""]

    var isUserLoggedIn
        set(value) = sharedPreferences.set(Constants.PREFERENCE_USER_LOGGEDIN, value)
        get() = sharedPreferences[Constants.PREFERENCE_USER_LOGGEDIN, false]

    var appLanguage
        set(value) = sharedPreferences.set(Constants.PREFERENCE_SELECTED_LANGUAGE, value)
        get() = sharedPreferences[Constants.PREFERENCE_SELECTED_LANGUAGE, "hi"]

    /**
     * Token returned by Vehicle EOL Login API (external Mahindra login).
     * Internally stored using KEY_ACCESS_TOKEN in a single preference file.
     */
    var vehicleEolJwtToken
        set(value) = sharedPreferences.set(Constants.KEY_ACCESS_TOKEN, value)
        get() = sharedPreferences[Constants.KEY_ACCESS_TOKEN, ""]

    fun clearSharedPreference() {
        sharedPreferences.edit().clear().apply()
    }
}
