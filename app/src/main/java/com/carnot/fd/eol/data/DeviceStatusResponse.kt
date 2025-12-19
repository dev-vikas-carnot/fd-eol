package com.carnot.fd.eol.data

data class DeviceStatusResponse(
    val gps: Boolean,
    val gsm: Boolean,
    val battery: Boolean,
    val fuel: Boolean,
    var activation_id:Int = 0,
    val imei:String,
    val iccid:String,
//    val vin:String,
)