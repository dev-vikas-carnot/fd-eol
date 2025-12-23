package com.carnot.fd.eol.features.vehicle_mapping.data

import com.carnot.fd.eol.data.VehicleMappingData


data class DeviceMappingRequest(
    val mappingData: List<DeviceMappingData>
)

data class DeviceMappingData(

    val IMEI: String,
    val ICCID: String,

    val devPlantID: String,          // e.g. NIPPON_01
    val devEOLStatus: Boolean,       // true = pass

    val deviceVariant: String,       // LOGGER
    val deviceVersion: String,       // CCU3.0

    val devEOLDateTime: Long,        // epoch seconds
    val currentSimBatchID: String,

    val deviceMappedDateTime: Long   // epoch seconds
)
