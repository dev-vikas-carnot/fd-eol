package com.carnot.fd.eol.module
import android.content.Context
import com.carnot.fd.eol.features.printer.data.BluetoothPrinterEntity
import com.carnot.fd.eol.features.printer.data.PrinterEntity
import com.carnot.fd.eol.features.printer.data.WifiPrinterEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PrinterFactory @Inject constructor(@ApplicationContext private val context: Context) {
    fun createPrinter(printerEntity: PrinterEntity): AbstractPrinter {

        return when (printerEntity) {

            is WifiPrinterEntity -> {
                TcpPrinter(printerEntity)
            }

            is BluetoothPrinterEntity -> {
                BluetoothPrinter(printerEntity, context)
            }
            else -> {
                throw Exception("Error unable to specific the printer type")
            }

        }


    }

}