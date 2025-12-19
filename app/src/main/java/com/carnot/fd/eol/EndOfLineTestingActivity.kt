package com.carnot.fd.eol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.carnot.fd.eol.data.ViewState
import com.carnot.fd.eol.databinding.ActivityEndOfLineTestingBinding
import com.carnot.fd.eol.features.printer.PrinterViewModel
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_EOL_API_FAILURE
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_EOL_API_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_EOL_SCREEN_VIEWED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_FLAGS_API_FAILURE
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_FLAGS_API_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_MANUAL_ENTRY
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_PRINT_BUTTON_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_IMEI2_CANCELLED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_IMEI2_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_IMEI2_FAILURE
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_VIN2_CANCELLED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_VIN2_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SCAN_VIN2_FAILURE
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_Step1_EOL
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_Submit_EOL
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_API
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_BACKEND
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_CLICK
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_VIEW
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_EOL
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents
import com.carnot.fd.eol.network.ApiResponse
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.LoggerHelper
import com.carnot.fd.eol.utils.PdfHelper
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.integration.android.IntentIntegrator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@AndroidEntryPoint
class EndOfLineTestingActivity : AppCompatActivity() {

    private lateinit var viewModel: EndOfLineTestingViewModel

    private val printerViewModel: PrinterViewModel by viewModels()

    /*Intent Handles For QR Code Scanning*/
    var scanIntent: IntentIntegrator? = null

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var customDialog: CustomDialog

    private var status: String = ""
    private var vin: String = ""
    private var imei: String = ""
    private var iccid: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityEndOfLineTestingBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_end_of_line_testing
        )

        viewModel = ViewModelProvider(this).get(EndOfLineTestingViewModel::class.java)

        val bundle = Bundle().apply {
            putString("event_type", EVENT_TYPE_VIEW)
        }
        FirebaseAnalyticsEvents.logEvent(
            EVENT_EOL_SCREEN_VIEWED,
            SCREEN_EOL,bundle)


        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC
        ).enableAutoZoom().build()
        val scanner = GmsBarcodeScanning.getClient(this, options)
        // Initialize the loading dialog
        loadingDialog = LoadingDialog(this)

        // Initialize the custom dialog
        customDialog = CustomDialog(this)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Enable back button in the Action Bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        scanIntent = IntentIntegrator(this)

        binding.btnScanTractorImei.setOnClickListener {

            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
            }
            FirebaseAnalyticsEvents.logEvent( EVENT_SCAN_IMEI2_CLICKED, SCREEN_EOL,bundle)


            //qrCodeScan(1)
            scanner.startScan().addOnSuccessListener { barcode ->
                barcode.rawValue?.let { it1 -> viewModel.scanImei(it1) }
            }.addOnCanceledListener {
                // Task canceled
                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_API)
                }
                FirebaseAnalyticsEvents.logEvent(
                    EVENT_SCAN_IMEI2_CANCELLED,
                    SCREEN_EOL,bundle)

            }.addOnFailureListener { e ->
                // Task failed with an exception
                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_API)
                    putString("message", e.message.toString())
                    putString("error_message", e.localizedMessage?.toString())

                }
                FirebaseAnalyticsEvents.logEvent(
                    EVENT_SCAN_IMEI2_FAILURE,
                    SCREEN_EOL,bundle)
            }
        }

        viewModel.apiResponseStatus.observe(this) { response ->

            when (response) {
                is ApiResponse.Loading -> {
                    // loadingDialog.show("Submitting...")
                }

                is ApiResponse.Error -> {
                    // Dismiss loading dialog
                    //loadingDialog.dismiss()
                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_BACKEND)
                        putString("error_message", response.message)

                    }
                    FirebaseAnalyticsEvents.logEvent(
                        EVENT_FLAGS_API_FAILURE,
                        SCREEN_EOL,bundle)
                    // Show error message
                    Toast.makeText(this, "Error: ${response.message}", Toast.LENGTH_SHORT).show()

                }

                is ApiResponse.Success -> {

                    imei = response.data?.imei.toString()
//                    vin = response.data?.vin.toString()
                    iccid = response.data?.iccid.toString()

                    val bundle = Bundle().apply {
                        putString("type_scan", "scan_vin2")
                        putString("imei",imei )
//                        putString("vin", vin)
                        putString("iccid",iccid)
                        putString("event_type", EVENT_TYPE_API)
                    }
                    FirebaseAnalyticsEvents.logEvent( EVENT_FLAGS_API_SUCCESS, SCREEN_EOL,bundle)
                    // loadingDialog.dismiss()

                }
            }
        }

        viewModel.apiResponseSubmit.observe(this) { response ->

            when (response) {
                is ApiResponse.Loading -> {
                    loadingDialog.show("Submitting...")
                }

                is ApiResponse.Error -> {

                    status = "Fail"
                    // Dismiss loading dialog
                    loadingDialog.dismiss()

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("type_scan", "scan_vin2_api_called")
                        putString("vin2",vin)
                        putString("error_message",response.message)
                    }
                    FirebaseAnalyticsEvents.logEvent( EVENT_EOL_API_FAILURE, SCREEN_EOL,bundle)

                    if (response.message == "EOL already PASSED for this VIN") {
                        binding.btnRetry.visibility = View.GONE

                        customDialog.show(
                            icon = R.drawable.baseline_error_outline_24,
                            response.message,
                            message = ""
                        )
                    } else {
                        customDialog.show(icon = R.drawable.baseline_error_outline_24,
                            "End Of Line Testing Failed",
                            message = response.message,
                            shouldShowPrint = false,
                            onPrintClicked = {

                                onPrintButtonClick()
                            })
                    }
                }

                is ApiResponse.Success -> {
                    // NOTE: response.data here is the vehicle-mapping response payload; it does not reliably contain
                    // IMEI/VIN/ICCID fields. Use the ViewModel's current values instead.
                    imei = viewModel.imei.value.orEmpty()
                    vin = viewModel.vin.value.orEmpty()
                    status = "Pass"
                    loadingDialog.dismiss()

                    val bundle = Bundle().apply {
                        putString("type_scan", "scan_vin2")
                        putString("message",response.message)
                        putString("imei",imei)
                        putString("vin",vin)
                        putString("iccid",iccid)

                        putString("event_type", EVENT_TYPE_API)
                    }
                    FirebaseAnalyticsEvents.logEvent( EVENT_EOL_API_SUCCESS, SCREEN_EOL,bundle)
                    FirebaseAnalyticsEvents.logEvent( EVENT_Submit_EOL, SCREEN_EOL,bundle)

                    customDialog.show(icon = R.drawable.baseline_check_circle_outline_24,
                        "End Of Line Testing Passed",
                        message = response.message.toString(),
                        shouldShowPrint = true,
                        onPrintClicked = {

                            onPrintButtonClick()
                        })
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {

            printerViewModel.uiState.collectLatest {
                LoggerHelper.saveLogToFile(
                    applicationContext, "Printer connected, start creating PDF"
                )
                if (it.ipWifiPrinter != null) {
                    val pdfFilePath = PdfHelper.createSamplePdf(
                        this@EndOfLineTestingActivity, "$vin", imei, "$status"
                    )
                    if (pdfFilePath != null) {
                        val pdfFile = File(pdfFilePath)
                        printerViewModel.sendFileToPrinter(pdfFile.toUri())
                    } else {
                        Toast.makeText(
                            this@EndOfLineTestingActivity, "Error creating PDF", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }

        printerViewModel.printState.observe(this) {

            when (it.status) {
                ViewState.NetworkStatus.LOADING -> {
                    loadingDialog.show("Printing...")
                }

                ViewState.NetworkStatus.SUCCESS -> {
                    loadingDialog.dismiss()
                    Toast.makeText(this, it.data, Toast.LENGTH_SHORT).show()
                }

                ViewState.NetworkStatus.ERROR -> {
                    loadingDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }

                ViewState.NetworkStatus.MESSAGE -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }

        }

        binding.etImei.setOnEditorActionListener { textView, actionId, keyEvent ->


            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.scanImei(textView.text.toString())


                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_VIEW)
                    putString("vin2", textView.text.toString())
                }
                FirebaseAnalyticsEvents.logEvent(EVENT_MANUAL_ENTRY, SCREEN_EOL,bundle)

                hideKeyboard(binding.etImei)
                true
            } else {
                false
            }
        }

        binding.btnSubmitImei.setOnClickListener {
            val bundle1 = Bundle().apply {
                putString("type_scan", "scan_imei2")
                putString("imei",binding.etImei.text.toString())
                putString("event_type", EVENT_TYPE_API)
            }
            FirebaseAnalyticsEvents.logEvent( EVENT_SCAN_VIN2_CLICKED ,SCREEN_EOL,bundle1)


            viewModel.scanImei(binding.etImei.text.toString())

            val bundle = Bundle().apply {
                putString("type_scan", "scan_imei2")
                putString("imei",binding.etImei.text.toString())
                putString("event_type", EVENT_TYPE_API)
            }
            FirebaseAnalyticsEvents.logEvent( EVENT_Step1_EOL ,SCREEN_EOL,bundle)



            hideKeyboard(binding.etImei)
        }

        // --- Step 3 (VIN) wiring ---
        // DataBinding here is one-way (android:text="@{viewModel.vin}") so we must push user edits into VM.
        binding.etVin.doAfterTextChanged {
            // Only update VM when VIN step is actually enabled.
            val state = viewModel.uiState.value
            if (state is com.carnot.fd.eol.data.EolUiState.VinInputEnabled ||
                state is com.carnot.fd.eol.data.EolUiState.SubmitReady
            ) {
                viewModel.setVin(it?.toString()?.trim().orEmpty())
            }
        }

        binding.etVin.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.scanVin(textView.text.toString().trim())
                hideKeyboard(binding.etVin)
                true
            } else {
                false
            }
        }

        binding.btnSubmitVin.setOnClickListener {
            viewModel.scanVin(binding.etVin.text.toString().trim())
            hideKeyboard(binding.etVin)
        }

        binding.btnScanVin.setOnClickListener {
            scanner.startScan().addOnSuccessListener { barcode ->
                barcode.rawValue?.let { raw -> viewModel.scanVin(raw.trim()) }
            }.addOnCanceledListener {
                // no-op
            }.addOnFailureListener {
                // no-op
            }
        }
    }

    fun hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun onPrintButtonClick(): Unit {
        LoggerHelper.saveLogToFile(this, "onPrintButtonClick")

        val bundle = Bundle().apply {
            putString("event_type", EVENT_TYPE_CLICK)
            putString("vin", vin)
            putString("imei", imei)
            putString("iccid", iccid)
            putString("status", status)
            putString("plantip", Globals.getPlantIp())

        }
        FirebaseAnalyticsEvents.logEvent( EVENT_PRINT_BUTTON_CLICKED ,SCREEN_EOL,bundle)

        Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
//        printerViewModel.fetchBluetoothPrinters()
        //     printerViewModel.fetchWifiPrinters()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Call your suspend function to connect to the printer
//                printerViewModel.sendToPrinterStatus(vin, imei, iccid, status, Globals.getPlantIp(), noOfPrint = 2)
                printerViewModel.sendToPrinterStatus(
                    vin,
                    imei,
                    iccid,
                    status,
                    2
                )


                /*
                // Switch to the main thread for UI updates
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EndOfLineTestingActivity, "Printer connected", Toast.LENGTH_SHORT).show()
                }
                 */
            } catch (e: Exception) {

                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_BACKEND)
                    putString("error_message", e.message.toString())
                }
                FirebaseAnalyticsEvents.logEvent( EVENT_PRINT_BUTTON_CLICKED ,SCREEN_EOL,bundle)



                // Handle any errors
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EndOfLineTestingActivity, "Error: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }

                FirebaseAnalyticsEvents.logError(EVENT_PRINT_BUTTON_CLICKED, e, "connect to the printer failure")


            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val result = data?.getStringExtra("SCAN_RESULT") ?: ""
            when (requestCode) {
                1 -> {
                    viewModel.scanImei(result)
                    imei = result
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // Handle the back button
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun qrCodeScan(requestCode: Int) {
        scanIntent?.setOrientationLocked(true)
        scanIntent?.setPrompt("Scan a barcode or QR Code")
        scanIntent?.setRequestCode(requestCode)
        scanIntent?.initiateScan()
    }

}