package com.carnot.fd.eol.data

data class CreateDeviceRequest(val imei: String, val iccid: String, val vin: String, val tractor_model_id: Int?)
