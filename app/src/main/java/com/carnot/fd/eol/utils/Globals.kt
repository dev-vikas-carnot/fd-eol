package com.carnot.fd.eol.utils

import android.content.Context
import com.carnot.fd.eol.utils.Constants.DEFAULT_IP
import com.carnot.fd.eol.utils.Constants.PLANT_1_IP
import com.carnot.fd.eol.utils.Constants.PLANT_1_KEY
import com.carnot.fd.eol.utils.Constants.PLANT_1_NAME
import com.carnot.fd.eol.utils.Constants.PLANT_2_IP
import com.carnot.fd.eol.utils.Constants.PLANT_2_KEY
import com.carnot.fd.eol.utils.Constants.PLANT_2_NAME
import com.carnot.fd.eol.utils.Constants.PLANT_3_IP
import com.carnot.fd.eol.utils.Constants.PLANT_3_KEY
import com.carnot.fd.eol.utils.Constants.PLANT_3_NAME
import com.carnot.fd.eol.utils.Constants.PLANT_DEFAULT_KEY

object Globals {

    val plantIps:HashMap<String,String> = HashMap<String, String>().apply {
        put(PLANT_1_KEY,PLANT_1_IP)
        put(PLANT_2_KEY,PLANT_2_IP)
        put(PLANT_3_KEY,PLANT_3_IP)
        put(PLANT_DEFAULT_KEY, DEFAULT_IP) // Default entry
    }

    //✅ Function to Get Plant Name by IP:
    fun getPlantKeyByIp(loggedInIp: String): String {
        return plantIps.entries.find { it.value == loggedInIp }?.key ?: PLANT_DEFAULT_KEY
    }

    fun formatPlantKey(rawKey: String): String {
        return rawKey.replaceFirstChar { it.uppercase() }
    }


    val plantDisplayNames: Map<String, String> = mapOf(
        PLANT_1_KEY to PLANT_1_NAME,
        PLANT_2_KEY to PLANT_2_NAME,
        PLANT_3_KEY to PLANT_3_NAME,
        PLANT_DEFAULT_KEY to "Unknown Plant"
    )

    fun getPlantDisplayNameByIp(ip: String): String {
        val key = getPlantKeyByIp(ip)
        return plantDisplayNames[key] ?: "Unknown Plant"
    }



    /***
     * JWT ACCESS TOKEN AND REFRESH TOKEN
     */

    @kotlin.jvm.JvmStatic
    fun getJWTRefreshToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        )
        return sharedPreferences.getString(Constants.KEY_REFRESH_TOKEN, "")
    }

    @kotlin.jvm.JvmStatic
    fun isValidIp(ip: String): Boolean {
        val regex =
            Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
        return regex.matches(ip)
    }

    @kotlin.jvm.JvmStatic
    fun setJWTRefreshToken(context: Context, token: String?) {
        val sharedPreferences = context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putString(Constants.KEY_REFRESH_TOKEN, token)
        editor.apply()
    }


    @kotlin.jvm.JvmStatic
    fun getJWTAccessToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        )
        return sharedPreferences.getString(Constants.KEY_ACCESS_TOKEN, "")
    }

    @kotlin.jvm.JvmStatic
    fun setJWTAccessToken(context: Context, token: String?) {
        val sharedPreferences = context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putString(Constants.KEY_ACCESS_TOKEN, token)
        editor.apply()
    }


    private const val KEY_EO_ACCESS_TOKEN = "fd_eol_access_token"
    private const val KEY_EO_REFRESH_TOKEN = "fd_eol_refresh_token"

    @JvmStatic
    fun getEolAccessToken(context: Context): String? =
        context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        ).getString(KEY_EO_ACCESS_TOKEN, null)

    @JvmStatic
    fun setEolAccessToken(context: Context, token: String?) {
        context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        ).edit().putString(KEY_EO_ACCESS_TOKEN, token).apply()
    }

    @JvmStatic
    fun getEolRefreshToken(context: Context): String? =
        context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        ).getString(KEY_EO_REFRESH_TOKEN, null)

    @JvmStatic
    fun setEolRefreshToken(context: Context, token: String?) {
        context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        ).edit().putString(KEY_EO_REFRESH_TOKEN, token).apply()
    }

    @JvmStatic
    fun clearEolTokens(context: Context) {
        context.getSharedPreferences(
            Constants.COM_CARNOT_KRISHI_FIELD_USER_DETAILS,
            Context.MODE_PRIVATE
        ).edit()
            .remove(KEY_EO_ACCESS_TOKEN)
            .remove(KEY_EO_REFRESH_TOKEN)
            .apply()
    }

    fun isVinValidString(input: String): Boolean {

        val vinPatternNew = Regex("^MBNC[A-Z]49S[A-Z][A-Z]{3}[0-9]{5}\$")
        if (vinPatternNew.matches(input))
            return true

        // Check if the length is 16 or 17
        if (input.length != 16 && input.length != 17) {
            return false
        }

        // Check if the first three characters are "MBN"
        if (!input.startsWith("MBN")) {
            return false
        }

        // Extract the 4th and 5th characters
        val substring =input.substring(3, 5)  // 4th and 5th characters

        // Check if the substring matches one of the allowed values
        val allowedValues = setOf("AV", "BU", "LW", "BG", "BN", "BS")
        if(substring !in allowedValues){

            if (input[3] != 'N' || !input[4].isLetter() || !input[4].isUpperCase()) {
                return false
            }
            // Check if the 4th character is 'N'
       }

        // Check if the first five characters are alphabetic (A-Z) and uppercase
        if (!input.substring(0, 5).all { it.isLetter() && it.isUpperCase() }) {
            return false
        }

        // Check if the 6th and 7th characters are digits (0-9)
        if (!input.substring(5, 7).all { it.isDigit() }) {
            return false
        }

        // Check if the 8th character is 'S'
        if (input[7] != 'S') {
            return false
        }

        // Check if the 9th character is an uppercase letter (A-Z)
        if (!input[8].isLetter() || !input[8].isUpperCase()) {
            return false
        }

        // All checks passed
        return true
    }

    fun getPlantIp(): String {
        val plantIpKey = getPlantIpKey()
        return plantIps[plantIpKey] ?: DEFAULT_IP // Return default IP if key is not found
    }

    // Function to determine the plant key based on the username
    private fun getPlantIpKey(): String {
        val userName = PreferenceUtil.userName ?: "" // Handle null case safely

        return when {
            userName.contains(PLANT_1_KEY, ignoreCase = true) -> PLANT_1_KEY
            userName.contains(PLANT_2_KEY, ignoreCase = true) -> PLANT_2_KEY
            userName.contains(PLANT_3_KEY, ignoreCase = true) -> PLANT_3_KEY
            else -> PLANT_DEFAULT_KEY // Return default plant key
        }
    }

}