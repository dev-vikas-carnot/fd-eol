package com.carnot.fd.eol.data

data class CreateDeviceResponse(
    val device_id: Int,
    val activation_id: Int,
    val tractor_model_id: Int,
    val tractor_model_params_id: Int,
    val hw_batch: Int
)