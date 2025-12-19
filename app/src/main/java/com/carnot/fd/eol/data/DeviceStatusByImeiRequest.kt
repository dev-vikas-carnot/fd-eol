package com.carnot.fd.eol.data

/**
 * Request payload for device status (packet validation) by IMEI.
 * Mirrors the existing DeviceStatusRequest but keyed by IMEI.
 */
data class DeviceStatusByImeiRequest(
    val IMEI: String
)



