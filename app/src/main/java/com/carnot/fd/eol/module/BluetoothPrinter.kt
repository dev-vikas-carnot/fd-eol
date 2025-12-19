package com.carnot.fd.eol.module

import android.content.Context
import android.util.Log
import com.carnot.fd.eol.features.printer.data.BluetoothPrinterEntity
import com.zebra.sdk.btleComm.BluetoothLeConnection
import kotlin.coroutines.suspendCoroutine

class BluetoothPrinter(
    private val printerEntity: BluetoothPrinterEntity,
    private val context: Context,
) : AbstractPrinter() {


    override suspend fun createConnection(): BluetoothPrinter = suspendCoroutine {

        connection = BluetoothLeConnection(printerEntity.mac, context)

        it.resumeWith(Result.success(this))

        Log.i("PrinterConnection", "Bluetooth connection created")
    }
    override suspend fun connect() = suspendCoroutine {
        connection.open()

        it.resumeWith(Result.success(true))
        Log.i("PrinterConnection", "Bluetooth connected")



    }


}