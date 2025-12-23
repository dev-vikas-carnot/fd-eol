package com.carnot.fd.eol.features.printer

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.carnot.fd.eol.data.ViewState
import com.carnot.fd.eol.features.printer.data.BluetoothPrinterEntity
import com.carnot.fd.eol.features.printer.data.PrinterEntity
import com.carnot.fd.eol.features.printer.data.PrinterRepository
import com.carnot.fd.eol.features.printer.data.WifiPrinterEntity
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_PRINTER_TEST_STARTED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_VIEW
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_HOME
import com.carnot.fd.eol.utils.LoggerHelper
import com.carnot.fd.eol.utils.PrinterPrefs
import com.carnot.fd.eol.utils.PrinterTestStatus
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.TcpConnection
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

data class PrintersUiState(
    val ipWifiPrinter: PrinterEntity? = null,
    val bluetoothMac: PrinterEntity? = null,
    val status1: String = "Disconnected",
    val status2: String = "Disconnected",
    val userMessage: String? = null,
    val isLoading: Boolean = false,
)

data class PrinterState(
    val printersEntity: List<PrinterEntity> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class PrinterViewModel @Inject constructor(
    val applicationContext: Application,
    private val printerRepository: PrinterRepository,
    private val dispatcher: CoroutineDispatcher
) : AndroidViewModel(applicationContext) {

    /** Printer test UI state (DialogFragment observes this) */
    val printerTestStatus =
        MutableLiveData<PrinterTestStatus>(PrinterTestStatus.Idle)

    private val _printerIp = MutableStateFlow(
        PrinterPrefs.getLastIp(applicationContext)
    )
    val printerIp = _printerIp.asStateFlow()

    /** Existing UI state */
    private val _uiState = MutableStateFlow(PrintersUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiConnectionState =
        MutableStateFlow(PrinterState(isLoading = true))
    val uiConnectionState = _uiConnectionState.asStateFlow()

    /** Existing print result state (Activity observes this) */
    val printState = MutableLiveData<ViewState<String?>>()

    /* -------------------------------------------------- */
    /* Entry point from UI                                */
    /* -------------------------------------------------- */

    fun testPrinterWithIp(ipAddress: String, noOfPrint: Int = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            printerTestStatus.postValue(PrinterTestStatus.Connecting)
            try {
                connectToPrinterTestPrint(ipAddress, noOfPrint)
            } catch (e: Exception) {
                printerTestStatus.postValue(
                    PrinterTestStatus.Error(e.message ?: "Unknown printer error")
                )
            }
        }
    }

    /* -------------------------------------------------- */
    /* Core printer test logic (unchanged, enhanced only) */
    /* -------------------------------------------------- */

    suspend fun connectToPrinterTestPrint(
        ipAddress: String,
        noOfPrint: Int = 1
    ) {
        val connection: Connection = TcpConnection(ipAddress, 9100)

        printState.postValue(ViewState.loading(null, "loading"))

        try {
            connection.open()

            val printer: ZebraPrinter =
                ZebraPrinterFactory.getInstance(connection)

            var printerStatus: PrinterStatus = printer.currentStatus

            // FirebaseAnalyticsEvents.logEvent(
//                EVENT_PRINTER_TEST_STARTED,
//                SCREEN_HOME,
//                Bundle().apply {
//                    putString("event_type", EVENT_TYPE_VIEW)
//                    putString("status", printerStatus.isReadyToPrint.toString())
//                }
//            )

            if (!printerStatus.isReadyToPrint) {
                printerTestStatus.postValue(
                    PrinterTestStatus.Error("Printer not ready")
                )
                printState.postValue(
                    ViewState.error(null, "Printer not ready")
                )
                return
            }

            printerTestStatus.postValue(PrinterTestStatus.Ready)

            val formattedDate =
                SimpleDateFormat("dd-MM-yyyy").format(Date())

//            val zplData =
//                "^XA\n" +
//                        "^PQ$noOfPrint,0,1,Y^FS\n" +
//                        "^FO50,50^ADN,36,20^FDIMEI: 123456789012345^FS\n" +
//                        "^FO50,100^ADN,36,20^FDICCID: 8912345678901234567^FS\n" +
//                        "^FO50,150^ADN,36,20^FDVIN: 1HGCM82633A123456^FS\n" +
//                        "^FO50,200^ADN,36,20^FDRESULT: PASS^FS\n" +
//                        "^FO50,250^ADN,36,20^FDDate: $formattedDate^FS\n" +
//                        "^FO720,20^BQN,2.0,10\n" +
//                        "^FH_^FDMM,A,IMEI:123456789012345,_0d_0aICCID:8912345678901234567,_0d_0aVIN:1HGCM82633A123456,_0d_0aRESULT:PASS,_0d_0aDATE:$formattedDate^FS\n" +
//                        "^XZ"
            val zplData =
                "^XA\n" +
                        "^PW600\n" +               // Label width (203 DPI, ~3 inch)
                        "^LL400\n" +               // Label length
                        "^FO20,20^A0N,30,30^FDTEST PRINT - GT800^FS\n" +
                        "^FO20,70^A0N,28,28^FDIMEI: 123456789012345^FS\n" +
                        "^FO20,110^A0N,28,28^FDICCID: 8912345678901234567^FS\n" +
                        "^FO20,150^A0N,28,28^FDVIN: 1HGCM82633A123456^FS\n" +
                        "^FO20,190^A0N,28,28^FDRESULT: PASS^FS\n" +
                        "^FO20,230^A0N,28,28^FDDate: $formattedDate^FS\n" +
                        "^FO350,60^BQN,2,6^FDLA,IMEI:123456789012345^FS\n" +
                        "^XZ"

            printerTestStatus.postValue(PrinterTestStatus.Printing)
            connection.write(zplData.toByteArray())

            val timeout = 30_000L
            val start = System.currentTimeMillis()

            while (System.currentTimeMillis() - start < timeout) {
                printerStatus = printer.currentStatus
                if (!printerStatus.isReadyToPrint) {
                    printerTestStatus.postValue(
                        PrinterTestStatus.Error("GT800 not ready (check media / ribbon)")
                    )
                    return
                }

                if (printerStatus.labelsRemainingInBatch <= 0 &&
                    printerStatus.isReadyToPrint
                ) {
                    printState.postValue(
                        ViewState.success(
                            "Data Sent successfully",
                            message = "Data Sent successfully"
                        )
                    )
                    return
                }
                delaySafe()
            }

            printState.postValue(
                ViewState.success(
                    "Please check printer outputs",
                    message = "Please check printer outputs"
                )
            )

        } catch (e: ZebraPrinterLanguageUnknownException) {
            handleError(e)
        } catch (e: Exception) {
            handleError(e)
        } finally {
            connection.close()
        }
    }

    /* -------------------------------------------------- */
    /* Helpers                                            */
    /* -------------------------------------------------- */

    private fun handleError(e: Exception) {
        LoggerHelper.saveLogToFile(applicationContext, "Printer error ${e.message}")
        printerTestStatus.postValue(
            PrinterTestStatus.Error(e.message ?: "Printer error")
        )
        printState.postValue(e.message?.let { ViewState.error(null, it) })
//        logCrashError(
//            apiName = "PrinterTest",
//            error = e,
//            message = "Printer test failed"
//        )
    }

    private suspend fun delaySafe() {
        withContext(Dispatchers.IO) {
            Thread.sleep(500)
        }
    }

    /* -------------------------------------------------- */
    /* Everything below remains unchanged (Wi-Fi / BT)   */
    /* -------------------------------------------------- */

    fun fetchBluetoothPrinters() = viewModelScope.launch {
        printerRepository.getBluetoothZebraPrinters()
            .onStart { _uiConnectionState.emit(PrinterState(isLoading = true)) }
            .catch { _uiConnectionState.emit(PrinterState(isLoading = false)) }
            .collectLatest {
                _uiConnectionState.emit(
                    PrinterState(
                        printersEntity = it.map { p ->
                            BluetoothPrinterEntity(
                                p.discoveryDataMap["DEVICE_UNIQUE_ID"].orEmpty(),
                                mac = p.address
                            )
                        }
                    )
                )
            }
    }

    fun fetchWifiPrinters() = viewModelScope.launch {
        printerRepository.getWifiZebraPrinters()
            .onStart { _uiConnectionState.emit(PrinterState(isLoading = true)) }
            .catch { _uiConnectionState.emit(PrinterState(isLoading = false)) }
            .collectLatest {
                _uiConnectionState.emit(
                    PrinterState(
                        printersEntity = it.map { p ->
                            WifiPrinterEntity(
                                p.discoveryDataMap["DEVICE_UNIQUE_ID"].orEmpty(),
                                ip = p.address,
                                port = p.discoveryDataMap["PORT_NUMBER"].orEmpty()
                            )
                        }
                    )
                )
            }
    }

    fun sendFileToPrinter(filePath: Uri) {
        viewModelScope.launch(dispatcher) {
            try {
                if (!printerRepository.isConnected()) {
                    printerRepository.connectToPrinterWithSuspend(
                        uiState.value.bluetoothMac!!
                    )
                }
                printerRepository.sendFileToPrinter(filePath).collectLatest {}
            } catch (e: Exception) {
//                logCrashError(
//                    apiName = "sendFileToPrinter",
//                    error = e,
//                    message = e.message ?: "File send failed"
//                )
            }
        }
    }

    private fun getActivePrinterIp(): String {
        return PrinterPrefs.getLastIp(applicationContext)
    }

    suspend fun sendToPrinterStatus(
        vin: String,
        imei: String,
        iccid: String,
        status: String,
        noOfPrint: Int = 1
    ) {
        val ipAddress = getActivePrinterIp()

        if (ipAddress == com.carnot.fd.eol.utils.Constants.DEFAULT_IP) {
            printState.postValue(
                ViewState.error(null, "Printer not configured. Please test printer first.")
            )
            return
        }

        val connection: Connection =
            TcpConnection(ipAddress, com.carnot.fd.eol.utils.Constants.PRINTER_PORT)

        printState.postValue(ViewState.loading(null, "Printing"))

        try {
            connection.open()

            val printer: ZebraPrinter =
                ZebraPrinterFactory.getInstance(connection)

            var printerStatus: PrinterStatus = printer.currentStatus

            if (!printerStatus.isReadyToPrint) {
                printState.postValue(
                    ViewState.error(null, "Printer not ready")
                )
                return
            }

            val formattedDate =
                SimpleDateFormat("dd-MM-yyyy").format(Date())

            val zplData =
                "^XA\n" +
                        "^PQ$noOfPrint,0,1,Y^FS\n" +
                        "^FO50,50^ADN,36,20^FDIMEI: $imei^FS\n" +
                        "^FO50,100^ADN,36,20^FDICCID: $iccid^FS\n" +
                        "^FO50,150^ADN,36,20^FDVIN: $vin^FS\n" +
                        "^FO50,200^ADN,36,20^FDRESULT: $status^FS\n" +
                        "^FO50,250^ADN,36,20^FDDate: $formattedDate^FS\n" +
                        "^FO720,20^BQN,2.0,10\n" +
                        "^FH_^FDMM,A,IMEI:$imei,_0d_0aICCID:$iccid,_0d_0aVIN:$vin,_0d_0aRESULT:$status,_0d_0aDATE:$formattedDate^FS\n" +
                        "^XZ"

            connection.write(zplData.toByteArray())

            val timeout = 30_000L
            val start = System.currentTimeMillis()

            while (System.currentTimeMillis() - start < timeout) {
                printerStatus = printer.currentStatus
                if (printerStatus.labelsRemainingInBatch <= 0 &&
                    printerStatus.isReadyToPrint
                ) {
                    printState.postValue(
                        ViewState.success("Print completed", "Print completed")
                    )
                    return
                }
                Thread.sleep(500)
            }

            printState.postValue(
                ViewState.success("Please check printer output", "Please check printer output")
            )

        } catch (e: Exception) {
            LoggerHelper.saveLogToFile(
                applicationContext,
                "Printer error ${e.message}"
            )
            printState.postValue(
                ViewState.error(null, e.message ?: "Printer error")
            )
//            logCrashError(
//                apiName = "sendToPrinterStatus",
//                error = e,
//                message = "Failed to print"
//            )
        } finally {
            connection.close()
        }
    }

    fun updatePrinterIp(ip: String) {
        PrinterPrefs.saveLastIp(applicationContext, ip)
        _printerIp.value = ip
    }
}
