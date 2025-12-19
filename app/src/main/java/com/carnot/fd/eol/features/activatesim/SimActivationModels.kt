package com.carnot.fd.eol.features.activatesim

/**
 * Request payload models for the SIM Activation By IMEI API,
 * modeled from the provided Postman collection.
 */
data class SimActivationRequest(
    val typeOfRequest: String,
    val simStatusReqOption: String,
    val newSIMStatus: String,
    val mappingData: List<SimActivationMappingData>
)

data class SimActivationMappingData(
    val IMEI: String
)



