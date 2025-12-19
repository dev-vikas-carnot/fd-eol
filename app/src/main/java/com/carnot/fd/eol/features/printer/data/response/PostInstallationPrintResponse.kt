package com.carnot.fd.eol.features.printer.data.response

import com.google.gson.annotations.SerializedName

data class PostInstallationPrintResponse(
    val imei:String,
    val iccid:String,
    val vin:String,
    @SerializedName("eol_date")
    val eolDate:String,
    @SerializedName("eol_status")
    val eolStatus:Boolean
)