package com.carnot.fd.eol.utils
import android.content.Context
import androidx.core.content.edit

object PrinterPrefs {

    private const val PREF_NAME = "printer_prefs"
    private const val KEY_LAST_IP = "last_printer_ip"

    fun saveLastIp(context: Context, ip: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_LAST_IP, ip)
            }
    }

    fun getLastIp(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_IP, Constants.DEFAULT_IP)
            ?: Constants.DEFAULT_IP
    }
}

