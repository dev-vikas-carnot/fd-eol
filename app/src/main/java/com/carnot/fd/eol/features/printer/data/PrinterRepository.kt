package com.carnot.fd.eol.features.printer.data

import android.content.Context
import android.net.Uri
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents.logCrashError
import com.carnot.fd.eol.module.AbstractPrinter
import com.carnot.fd.eol.module.PrinterFactory
import com.carnot.fd.eol.utils.LoggerHelper
import com.zebra.sdk.btleComm.BluetoothLeDiscoverer
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveryHandler
import com.zebra.sdk.printer.discovery.NetworkDiscoverer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

class PrinterRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val factory: PrinterFactory
) {

    private lateinit var abstractPrinter: AbstractPrinter

    fun getBluetoothZebraPrinters() = callbackFlow {
        BluetoothLeDiscoverer.findPrinters(context, object : DiscoveryHandler {
            val discoveredPrinters: MutableList<DiscoveredPrinter> = mutableListOf()
            override fun foundPrinter(printer: DiscoveredPrinter) {
                println(printer)
                LoggerHelper.saveLogToFile(context,"Found Printer: ${printer.address}")
                discoveredPrinters.add(printer)
            }

            override fun discoveryFinished() {
                for (printer in discoveredPrinters) {
                    LoggerHelper.saveLogToFile(context,"Printer List: ${printer.address}")
                    println(printer)
                }
                trySend(discoveredPrinters)
            }

            override fun discoveryError(message: String) {

                LoggerHelper.saveLogToFile(context,"Discovery Error: ${message}")
                println("An error occurred during discovery : $message")
            }
        })
        awaitClose()
    }



    fun getWifiZebraPrinters() = callbackFlow {
        NetworkDiscoverer.findPrinters(object : DiscoveryHandler {
            val discoveredPrinters: MutableList<DiscoveredPrinter> = mutableListOf()
            override fun foundPrinter(printer: DiscoveredPrinter) {
                println(printer)
                LoggerHelper.saveLogToFile(context,"Printer found ${printer.address}")
                discoveredPrinters.add(printer)
            }

            override fun discoveryFinished() {
                for (printer in discoveredPrinters) {
                    LoggerHelper.saveLogToFile(context,"Printer discoveryFinished ${printer.address}")
                    println(printer)
                }
                trySend(discoveredPrinters)
            }

            override fun discoveryError(message: String) {
                println("An error occurred during discovery : $message")
                LoggerHelper.saveLogToFile(context,"Printer discoveryError $message")
            }
        })
        awaitClose()
    }




    suspend fun connectToPrinterWithSuspend(printer: PrinterEntity) {

        abstractPrinter = factory.createPrinter(printer)

        abstractPrinter.createConnection().connect()
    }


    fun connectToPrinter(printer: PrinterEntity) = flow {

        abstractPrinter = factory.createPrinter(printer)

        LoggerHelper.saveLogToFile(context,"Printer Connecting Start")
        emit("Connecting....")
        try {
            val isConnected = abstractPrinter.createConnection().connect()

            LoggerHelper.saveLogToFile(context,"Printer Connected : ${isConnected}" )

            emit(if (isConnected) "Connected...." else "Disconnected")
        } catch (e: ConnectionException) {
            LoggerHelper.saveLogToFile(context,"Error in Printer Connected: ${e.message}" )

            logCrashError(
                apiName = "Error while connectToPrinter function",
                error = e,
                message = e.message.toString()
            )

            emit("Error....${e.message}")
        }catch (e:Exception){
            logCrashError(
                apiName = "Error while connectToPrinter function",
                error = e,
                message = e.message.toString()
            )
        }

    }.flowOn(Dispatchers.IO)


    fun testPrinter() = CoroutineScope(dispatcher).launch {

        if (abstractPrinter.isConnected()) {
            abstractPrinter.testPrinter()
        }
    }

    fun isConnected(): Boolean = abstractPrinter.isConnected()


    fun sendFileToPrinter(filePath: Uri) =
        abstractPrinter.sendFile(context.contentResolver.openInputStream(filePath))


}