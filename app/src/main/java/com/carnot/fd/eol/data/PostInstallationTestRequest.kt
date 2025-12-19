package com.carnot.fd.eol.data

data class PostInstallationTestRequest(
    val vin: String,
    val activation_id: Int,
    val gps_lock_status: GpsLockStatus,
    val gsm_status: GsmStatus,
    val battery_voltage: BatteryVoltage,
    val latitude: Float?,  // Nullable to make it optional
    val longitude: Float?  // Nullable to make it optional
)

data class GpsLockStatus(
    val status: Boolean
)

data class GsmStatus(
    val status: Boolean
)

data class BatteryVoltage(
    val status: Boolean
)