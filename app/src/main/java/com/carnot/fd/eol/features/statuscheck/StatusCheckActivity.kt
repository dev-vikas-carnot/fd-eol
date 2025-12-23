package com.carnot.fd.eol.features.statuscheck

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.carnot.fd.eol.CustomDialog
import com.carnot.fd.eol.LoadingDialog
import com.carnot.fd.eol.R
import com.carnot.fd.eol.data.ViewState
import com.carnot.fd.eol.databinding.ActivityEolStatuschkBinding
import com.carnot.fd.eol.features.printer.PrinterViewModel
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_MANUAL_ENTRY
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCAN
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCANCANCELL
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCANFAIL
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCAN_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCAN_FAILURE
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCAN_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_SCREEN_VIEWED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_API
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_CLICK
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_VIEW
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_REPRINT
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_staTus
import com.carnot.fd.eol.network.ApiResponse
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.LoggerHelper
import com.carnot.fd.eol.utils.PdfHelper
import com.carnot.fd.eol.features.test.CameraXScannerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@AndroidEntryPoint
class StatusCheckActivity : AppCompatActivity() {

    private lateinit var  viewModel: StatusCheckViewModelViewModel

    private  val  printerViewModel: PrinterViewModel by viewModels()

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var customDialog: CustomDialog

    private var status:String = ""
    private var vin:String = ""
    private var imei:String = ""
    private var iccid:String = ""
    private var eolDate:String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityEolStatuschkBinding  = DataBindingUtil.setContentView(
            this, R.layout.activity_eol_statuschk
        )

        viewModel = ViewModelProvider(this).get(StatusCheckViewModelViewModel::class.java)

        val bundle = Bundle().apply {
            putString("event_type", EVENT_TYPE_VIEW)
        }
        // FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_SCREEN_VIEWED, SCREEN_REPRINT,bundle)


        // Initialize the loading dialog
        loadingDialog = LoadingDialog(this)

        // Initialize the custom dialog
        customDialog = CustomDialog(this)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Enable back button in the Action Bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Status Check"


        binding.scanQR.setOnClickListener {
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
            }
            // FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_SCAN, SCREEN_REPRINT,bundle)

            scanLauncher.launch(Intent(this, CameraXScannerActivity::class.java))
        }
        CoroutineScope(Dispatchers.Main).launch {
            printerViewModel.uiState.collectLatest {
                LoggerHelper.saveLogToFile(applicationContext,"Printer connected, start creating PDF")
                if(it.ipWifiPrinter!=null) {
                    val pdfFilePath = PdfHelper.createSamplePdf(
                        this@StatusCheckActivity,
                        "$vin",
                        imei,
                        "$status"
                    )
                    if (pdfFilePath != null) {
                        val pdfFile = File(pdfFilePath)
                        printerViewModel.sendFileToPrinter(pdfFile.toUri())
                    } else {
                        Toast.makeText(
                            this@StatusCheckActivity,
                            "Error creating PDF",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }

        printerViewModel.printState.observe(this){

            when(it.status){
                ViewState.NetworkStatus.LOADING->{
                    loadingDialog.show("Printing...")
                }
                ViewState.NetworkStatus.SUCCESS->{
                    loadingDialog.dismiss()
                    Toast.makeText(this, it.data, Toast.LENGTH_SHORT).show()
                }
                ViewState.NetworkStatus.ERROR->{
                    loadingDialog.dismiss()
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
                ViewState.NetworkStatus.MESSAGE->{
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }

        }
        viewModel.apiResponseSubmit.observe(this) { response ->

            when(response){
                is ApiResponse.Loading->{
                    loadingDialog.show("Submitting...")
                }
                is ApiResponse.Error -> {
                    // Dismiss loading dialog
                    loadingDialog.dismiss()

                    val errorMessage = when (response.message) {
                        "VIN not available in record" -> "EOL NOT ATTEMPTED"
                        "EOL not completed" -> "DEVICE LINKING DONE. EOL NOT DONE"
                        else -> response.message
                    }

                    customDialog.show(
                        icon = R.drawable.baseline_error_outline_24,
                        title = "Error",
                        message = errorMessage
                    )

                    /*    customDialog.show(icon = R.drawable.baseline_error_outline_24,
                            "End Of Line Testing",
                            message = response.message,)*/

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("error_message",response.message )
                    }
                    // FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_SCAN_FAILURE,SCREEN_REPRINT,bundle)

                }
                is ApiResponse.Success -> {

                    imei = response.data?.imei.toString()
                    vin = response.data?.vin.toString()
                    iccid = response.data?.iccid.toString()
                    val eolStatus = response.data?.eolStatus
                    eolDate = response.data?.eolDate.toString()


                    status = if(eolStatus == true){
                        "Pass"
                    }else{
                        "Fail"
                    }
                    loadingDialog.dismiss()

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("imei", imei)
                        putString("vin", vin)
                        putString("iccid", iccid)
                        putString("eolDate", eolDate)
                        putString("status", status)
                        putString("message",response.message )
                    }
                    // FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_SCAN_SUCCESS,SCREEN_REPRINT,bundle)

                    val customMessage = when (response.message.toString()) {
                        "VIN not available in record" -> "EOL NOT ATTEMPTED"
                        "EOL not completed" -> "DEVICE LINKING DONE. EOL NOT DONE"
                        else -> response.message.toString() // fallback to original message
                    }

                    customDialog.show(
                        icon = R.drawable.baseline_check_circle_outline_24,
                        title = "VIN :$vin",
                        message = customMessage,
                        shouldShowPrint = false,
                        onPrintClicked = {
                            onPrintButtonClick()
                        }
                    )
                }
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

                    // Show error message
                    Toast.makeText(this, "Error: ${response.message}", Toast.LENGTH_SHORT).show()

                }

                is ApiResponse.Success -> {

                }
            }
        }
        binding.etVin.setOnEditorActionListener { textView, actionId, keyEvent ->


            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_VIEW)
                    putString("vin", binding.etVin.text.toString())
                }
                // FirebaseAnalyticsEvents.logEvent(EVENT_MANUAL_ENTRY,SCREEN_REPRINT,bundle)

                viewModel.scanVin(textView.text.toString())
                hideKeyboard(binding.etVin)
                true
            } else {
                false
            }
        }

        binding.submitButton.setOnClickListener {
            val vin = binding.etVin.text.toString()
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
                putString("vin",vin )
            }
            // FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_CLICKED,SCREEN_REPRINT,bundle)
            viewModel.submit(vin)
        }
    }

    fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
    fun onPrintButtonClick():Unit{

        val bundle = Bundle().apply {
            putString("event_type", EVENT_TYPE_CLICK)
            putString("status",status)
            putString("imei",imei )
            putString("iccid",iccid )
            putString("vin",vin)
            putString("eoldate",eolDate)
            putString("plantip",Globals.getPlantIp())
        }
        // FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_SCAN_CLICKED,SCREEN_staTus,bundle)

        LoggerHelper.saveLogToFile(this,"onPrintButtonClick")
        Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
//        printerViewModel.fetchBluetoothPrinters()
   //     printerViewModel.fetchWifiPrinters()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Call your suspend function to connect to the printer
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
                // Handle any errors
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StatusCheckActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }

                // FirebaseAnalyticsEvents.logError(EVENT_REPRINT_CLICKED, e, "connect to the re print failure")

            }
        }
    }

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = result.data?.getStringExtra("SCAN_RESULT")
            if (!scanResult.isNullOrEmpty()) {
                viewModel.scanVin(scanResult)
                vin = scanResult
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


}