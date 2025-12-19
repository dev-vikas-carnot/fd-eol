package com.carnot.fd.eol.utils

sealed class PrinterTestStatus {
    object Idle : PrinterTestStatus()
    object Connecting : PrinterTestStatus()
    object Ready : PrinterTestStatus()
    object Printing : PrinterTestStatus()
    data class Error(val message: String) : PrinterTestStatus()
}
