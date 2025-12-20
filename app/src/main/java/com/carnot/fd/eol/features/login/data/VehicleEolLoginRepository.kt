package com.carnot.fd.eol.features.login.data

import android.app.Application
import com.carnot.fd.eol.network.VehicleEolLoginService
import com.google.gson.JsonObject

/**
 * Repository for the external Vehicle EOL Login API.
 *
 * Mirrors the existing repository pattern (e.g. LoginRepository) but simplified:
 * returns just the JWT/token string to be cached in preferences.
 */
class VehicleEolLoginRepository(
    private val application: Application
) {

    /**
     * Performs Vehicle EOL login and returns the JWT/token string if available.
     * Returns null on error or if the token field is not present.
     */
    suspend fun loginVehicleEol(): String? {
        val service = VehicleEolLoginService.create(application)
        val body = JsonObject().apply {
            addProperty("userName", "TESTINGVEHICLEEOL@gmail.com")
            addProperty("password", "Abc@123")
            addProperty("requestedFrom", "WEB")
            addProperty("vehicleCategory", "FD_DOM")
            addProperty("operationCategory", "VEHICLE_EOL")
        }

        val response = service.vehicleEolLogin(body)
        if (!response.isSuccessful) {
            return null
        }

        val json = response.body() ?: return null

        // Try common token field names; adjust as needed based on backend response.
        return when {
            json.has("token") -> json.get("token").asString
            else -> null
        }
    }
}



