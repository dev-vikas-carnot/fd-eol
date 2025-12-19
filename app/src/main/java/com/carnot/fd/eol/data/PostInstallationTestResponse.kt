package com.carnot.fd.eol.data

data class PostInstallationTestResponse(
    val activation_id: Int,
    val imei:String,
    val iccid:String,
    val vin:String,
)