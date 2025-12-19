package com.carnot.fd.eol.features.printer.data


abstract class PrinterEntity(
    val uniqueName: String
)

class WifiPrinterEntity(uniqueName: String, val ip: String, val port: String) :
    PrinterEntity(uniqueName)

class BluetoothPrinterEntity(uniqueName: String, val mac: String) : PrinterEntity(uniqueName)
