package com.carnot.fd.eol.data

/**
 * Request payload for EOL Vehicle Mapping API.
 * Mirrors the structure from the Postman "EOL Vehicle Mapping" example.
 */
data class VehicleMappingRequest(
    val mappingData: List<VehicleMappingData>
)

data class VehicleMappingData(
    val vin: String,
    val IMEI: String,
    val vehicleType: String,
    val vehicleModel: String,
    val vehiclePlantID: String,
    val vehicleCategory: String,      // e.g. "FD_DOM"
    val vehicleEOLStatus: Boolean,    // true for EOL pass
    val vehicleEOLDateTime: Long,
    val vehicleSoldCountry: String,   // e.g. "IND"
    val vinParsingRequired: Boolean,
    val vehicleMappedDateTime: Long
)



