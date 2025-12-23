package com.carnot.fd.eol

import com.carnot.fd.eol.features.vehicle_mapping.domain.TractorMasterRepository
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.carnot.fd.eol.databinding.ActivityMainBinding
import com.carnot.fd.eol.features.activatesim.SimActivationActivity
import com.carnot.fd.eol.features.faq.ui.FAQActivity
import com.carnot.fd.eol.features.login.data.VehicleEolLoginRepository
import com.carnot.fd.eol.features.login.ui.LoginActivity
import com.carnot.fd.eol.features.printer.PrinterIpDialogFragment
import com.carnot.fd.eol.features.printer.PrinterViewModel
import com.carnot.fd.eol.features.printer.ReprintActivity
import com.carnot.fd.eol.features.statuscheck.StatusCheckActivity
import com.carnot.fd.eol.features.test.CameraXScannerActivity
import com.carnot.fd.eol.features.vehicle_mapping.domain.TractorMasterCache
import com.carnot.fd.eol.features.vehicle_mapping.ui.EndOfLineTestingActivity
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_END_OF_LINE_TESTING_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_HOME_SCREEN_VIEWED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_MODULE_ALREADY_INSTALLED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_MODULE_INSTALLED_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_MODULE_INSTALL_ERROR
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_REPRINT_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SHARE_LOG_FILE_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SHARE_LOG_FILE_DIALOG_SHOWN
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SHARE_LOG_FILE_ERROR
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SHARE_LOG_FILE_NOT_FOUND
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SHARE_LOG_FILE_STARTED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SIM_ACTIVATION_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_STATUSCHK_CLICKED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_CLICK
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_VIEW
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_USER_LOGOUT
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_HOME
import com.carnot.fd.eol.utils.Constants
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.Globals.getPlantDisplayNameByIp
import com.carnot.fd.eol.utils.LoggerHelper
import com.carnot.fd.eol.utils.PdfHelper
import com.carnot.fd.eol.utils.PreferenceUtil
import com.carnot.fd.eol.utils.ViewUtils
import com.carnot.fd.eol.utils.apiCall
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    val printerViewModel: PrinterViewModel by viewModels()

    // Repository for external Vehicle EOL Login API
    private val vehicleEolLoginRepository by lazy {
        VehicleEolLoginRepository(application)
    }

    private lateinit var loadingDialog: LoadingDialog
    private val tractorRepo by lazy {
        TractorMasterRepository(applicationContext)
    }

//    private val tractorRepo = com.carnot.fd.eol.features.vehicle_mapping.domain.TractorMasterRepository(application)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PreferenceUtil.initPreference(this)
        ensureTractorMasterLoaded()

        val bundle = Bundle().apply {
            putString("event_type", EVENT_TYPE_VIEW)
        }
//        // FirebaseAnalyticsEvents.logEvent(EVENT_HOME_SCREEN_VIEWED, SCREEN_HOME, bundle)


        //startActivity(Intent(this,LoginActivity::class.java))
        // Ensure Vehicle EOL token is fetched and cached
        performVehicleEolLoginIfNeeded()
        requestCameraPermission()
        // LoggerHelper.saveLogToFile(this,"Sample Log")
        //  shareLogFile(this)

        // Initialize the loading dialog
        loadingDialog = LoadingDialog(this)
        val myCustomizedString = SpannableStringBuilder()
            .append("Welcome ")
            .append(PreferenceUtil.userName)
        binding.usernameWelcome.text = myCustomizedString



        binding.btnActivateSim.setOnClickListener {
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
            }
//            // FirebaseAnalyticsEvents.logEvent(EVENT_SIM_ACTIVATION_CLICKED, SCREEN_HOME, bundle)

            if (checkCameraPermission()) {
                startActivity(
                    Intent(
                        this,
                        SimActivationActivity::class.java
                    )
                )
            } else {
                Toast.makeText(this, "Camera permission not provided", Toast.LENGTH_SHORT).show()
            }

        }

        binding.btnEndOfLineTesting.setOnClickListener {
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
            }
//            // FirebaseAnalyticsEvents.logEvent(EVENT_END_OF_LINE_TESTING_CLICKED, SCREEN_HOME, bundle)

            if (checkCameraPermission()) {
                if (!TractorMasterCache.isAvailable()) {
                    loadTractorMasterAndProceed()
                } else {
                    startActivity(Intent(this, EndOfLineTestingActivity::class.java))
                }
            } else {
                Toast.makeText(this, "Camera permission not provided", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareLog.setOnClickListener {
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
            }
//            // FirebaseAnalyticsEvents.logEvent(EVENT_SHARE_LOG_FILE_CLICKED, SCREEN_HOME, bundle)

            /*
            val pdfFilePath = PdfHelper.createSamplePdf(this,"12230","12","Pass")
            if (pdfFilePath != null) {
                val pdfFile = File(pdfFilePath)
                openPdf(this, pdfFile)
            } else {
                Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show()
            }

             */
            shareLogFile(this)
        }

        binding.btnTestZebraPrinter.setOnClickListener {
            PrinterIpDialogFragment().show(
                supportFragmentManager,
                "PrinterIpDialog"
            )

//            showPrinterIpDialog()

//            val bundle = Bundle().apply {
//                putString("event_type", EVENT_TYPE_CLICK)
//            }
//            // FirebaseAnalyticsEvents.logEvent(EVENT_TEST_PRINTER_CLICKED,SCREEN_HOME,bundle)
//
//            LoggerHelper.saveLogToFile(this, "onPrintButtonClick")
//
//            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
////        printerViewModel.fetchBluetoothPrinters()
//            // Launch a coroutine to call the suspend function
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    // Call your suspend function to connect to the printer
//                    printerViewModel.connectToPrinterTestPrint(Globals.getPlantIp(),2)
//
//                    /*
//                    // Switch to the main thread for UI updates
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@MainActivity, "Printer connected", Toast.LENGTH_SHORT).show()
//                    }
//
//                     */
//                } catch (e: Exception) {
//                    // Handle any errors
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG)
//                            .show()
//                    }
//
//                    logCrashError(
//                        apiName = "connectToPrinter",
//                        error = e,
//                        message = "Failed to connect to the printer"
//                    )
//                }
//            }
        }

        binding.btnRePrint.setOnClickListener {
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
            }
//            // FirebaseAnalyticsEvents.logEvent(EVENT_REPRINT_CLICKED, SCREEN_HOME, bundle)

            startActivity(Intent(this@MainActivity, ReprintActivity::class.java))
        }

        binding.btnVerifyStatus.setOnClickListener {
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
            }
//            // FirebaseAnalyticsEvents.logEvent(EVENT_STATUSCHK_CLICKED, SCREEN_HOME, bundle)

            startActivity(Intent(this@MainActivity, StatusCheckActivity::class.java))

        }


        binding.eolHistory.setOnClickListener {

            val plantName = getPlantDisplayNameByIp(Globals.getPlantIp());


            Log.e("PLANT", "Logged-in plant is: $plantName")

            val intent = Intent(this, WebviewActivity::class.java).apply {
                putExtra("user_id", PreferenceUtil.userId)
                putExtra("user_name", PreferenceUtil.userName)
                putExtra("plant_name", plantName)
            }
            startActivity(intent)


        }

        observeChanges()
        initScanner()

        askNotificationPermission()

    }

    /**
     * Calls Vehicle EOL Login API (external Mahindra endpoint) and saves
     * the returned token into shared preferences, if not already present.
     */
    private fun performVehicleEolLoginIfNeeded() {
        // If we already have a token, skip login
        if (!Globals.getEolAccessToken(this).isNullOrEmpty()) {
            Timber.d("Inside performVehicleEolLoginIfNeeded token already present")
            return
        }
        Timber.d("Inside performVehicleEolLoginIfNeeded token not present call api");

        CoroutineScope(Dispatchers.IO).launch {
            apiCall(
                execute = {
                    val token = vehicleEolLoginRepository.loginVehicleEol()
                    if (!token.isNullOrEmpty()) {
                        Globals.setEolAccessToken(this@MainActivity, token)
                    } else {
                        LoggerHelper.saveLogToFile(
                            this@MainActivity,
                            "VehicleEolLogin failed or returned empty token"
                        )
                    }
                },
                onNoInternet = {
                    LoggerHelper.saveLogToFile(
                        this@MainActivity,
                        "VehicleEolLogin: No internet"
                    )
                },
                onException = {
                    LoggerHelper.saveLogToFile(
                        this@MainActivity,
                        "VehicleEolLogin exception: ${it.message}"
                    )
                }
            )
        }
    }

    private fun initScanner() {
        // Scanner module installation removed as we are using CameraX
    }

//    override fun onResume() {
//        super.onResume()
//        val ip = PrinterPrefs.getLastIp(this)
//        binding.tvPrinterStatus.text =
//            if (ip == Constants.DEFAULT_IP)
//                "Printer: Not configured"
//            else
//                "Printer connected: $ip"
//    }

    fun observeChanges() {
        CoroutineScope(Dispatchers.Main).launch {
            printerViewModel.uiState.collectLatest {
                LoggerHelper.saveLogToFile(
                    applicationContext,
                    "Printer connected, start creating PDF"
                )
                if (it.ipWifiPrinter != null) {
                    val pdfFilePath = PdfHelper.createSamplePdf(
                        this@MainActivity,
                        "TEST",
                        "TEST",
                        "TEST"
                    )
                    if (pdfFilePath != null) {
                        val pdfFile = File(pdfFilePath)
                        printerViewModel.sendFileToPrinter(pdfFile.toUri())
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error creating PDF",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }

        lifecycleScope.launch {
            printerViewModel.printerIp.collectLatest { ip ->
                binding.tvPrinterStatus.text =
                    if (ip.isBlank() || ip == Constants.DEFAULT_IP) {
                        "Printer: Not configured"
                    } else {
                        "Printer connected: $ip"
                    }
            }
        }


//        printerViewModel.printState.observe(this) {
//
//            when (it.status) {
//                ViewState.NetworkStatus.LOADING -> {
//                    loadingDialog.show("Checking...Please wait!!!")
//                }
//
//                ViewState.NetworkStatus.SUCCESS -> {
//                    loadingDialog.dismiss()
//
//                    val bundle = Bundle().apply {
//                        putString("event_type", EVENT_TYPE_API)
//                        putString("status_data", it.data)
//                    }
//                    // FirebaseAnalyticsEvents.logEvent( EVENT_PRINTER_TEST_SUCCESS,  SCREEN_HOME, bundle)
//
//                    Toast.makeText(this, it.data, Toast.LENGTH_SHORT).show()
//                }
//
//                ViewState.NetworkStatus.ERROR -> {
//                    loadingDialog.dismiss()
//                    val bundle = Bundle().apply {
//                        putString("event_type", EVENT_TYPE_API)
//                        putString("error_message", it.message)
//                    }
//                    // FirebaseAnalyticsEvents.logEvent( EVENT_PRINTER_TEST_FAILURE,  SCREEN_HOME, bundle)
//
//                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
//                }
//
//                ViewState.NetworkStatus.MESSAGE -> {
//                    val bundle = Bundle().apply {
//                        putString("event_type", EVENT_TYPE_API)
//                        putString("error_message", it.message)
//                    }
//                    // FirebaseAnalyticsEvents.logEvent( EVENT_PRINTER_TEST_STARTED,  SCREEN_HOME, bundle)
//                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
//                }
//
//
//                else -> {}
//            }
//
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.logout -> {

                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_CLICK)
                }
                // FirebaseAnalyticsEvents.logEvent(EVENT_USER_LOGOUT, SCREEN_HOME, bundle)


                TractorMasterCache.clear()
                PreferenceUtil.clearSharedPreference()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return true
            }

            R.id.help -> {

                startActivity(Intent(this, FAQActivity::class.java))
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private val requestCameraPermissionLauncher = this.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, initiate QR scan
        } else {
            // Permission denied, show toast
            Toast.makeText(this, "Camera permission not provided", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@MainActivity,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.CAMERA
            )
        ) {
            // Show an explanation to the user why the permission is needed
            Toast.makeText(this, "Camera permission is needed to scan QR codes", Toast.LENGTH_LONG)
                .show()
        }
        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    fun openPdf(context: Context, pdfFile: File) {
        val pdfUri: Uri =
            // Use FileProvider for Android N and above
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    fun shareLogFile(context: Context) {
        // Start Firebase Performance Trace
        val trace = Firebase.performance.newTrace("share_log_file_trace")
        trace.start()

        // Log Share Action Started Event
        val bundle = Bundle().apply {
            putString("action", "share_log_file")
            putString("status", "started")
        }
        // FirebaseAnalyticsEvents.logEvent(EVENT_SHARE_LOG_FILE_STARTED, SCREEN_HOME, bundle)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(context.getExternalFilesDir(null), "logs.txt")
                // Check if Log File Exists
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Log file not found.", Toast.LENGTH_SHORT).show()
                        // Log Event: Log File Not Found
                        bundle.putString("status", "not_found")
                        // FirebaseAnalyticsEvents.logEvent(
//                            EVENT_SHARE_LOG_FILE_NOT_FOUND,
//                            SCREEN_HOME,
//                            bundle
//                        )
                    }
                    trace.stop()
                    return@launch
                }

                val fileUri =
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Share Log File")
                chooserIntent.putExtra(Intent.EXTRA_CHOSEN_COMPONENT, shareIntent)

                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)

                    chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INITIAL_INTENTS)?.let {
                        val selectedPackage =
                            it.resolveActivity(context.packageManager)?.packageName ?: "unknown"
                        bundle.putString("status", "dialog_shown")
                        bundle.putString("package_name", selectedPackage)
                        // FirebaseAnalyticsEvents.logEvent(
//                            EVENT_SHARE_LOG_FILE_DIALOG_SHOWN,
//                            SCREEN_HOME,
//                            bundle
//                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("Log", "Error sharing log file: ${e.message}")

                // Log Error Event
                bundle.putString("status", "error")
                bundle.putString("error_message", e.message)

                // FirebaseAnalyticsEvents.logEvent(EVENT_SHARE_LOG_FILE_ERROR, SCREEN_HOME, bundle)

//                logCrashError(
//                    apiName = "shareLogFile",
//                    error = e,
//                    message = "Failed to shareLogFile"
//                )
            } finally {
                trace.stop()
            }
        }
    }


    //Required for chucker
    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Can post notifications.
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // Display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Can post notifications.
        } else {
            // Inform user that that your app will not show notifications.
        }
    }

//    private fun showPrinterIpDialog() {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_printer_ip, null)
//        val etIp = dialogView.findViewById<EditText>(R.id.etPrinterIp)
//        val progress = dialogView.findViewById<ProgressBar>(R.id.progress)
//
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Test Zebra Printer")
//            .setView(dialogView)
//            .setCancelable(false)
//            .setPositiveButton("CONNECT", null)
//            .setNegativeButton("CANCEL") { d, _ -> d.dismiss() }
//            .create()
//
//        dialog.setOnShowListener {
//            val connectBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
//
//            connectBtn.setOnClickListener {
//                val ip = etIp.text.toString().trim()
//
//                if (ip.isEmpty()) {
//                    etIp.error = "Printer IP required"
//                    return@setOnClickListener
//                }
//
//                progress.visibility = View.VISIBLE
//                connectBtn.isEnabled = false
//
//                // 🔹 Call ViewModel wrapper (NON-suspend)
//                printerViewModel.testPrinterWithIp(ip, 1)
//            }
//        }
//
//        dialog.show()
//
//        // 🔹 Dialog reacts to printer state via EXISTING observer
//        printerViewModel.printState.observe(this) { state ->
//            when (state.status) {
//                ViewState.NetworkStatus.LOADING -> {
//                    progress.visibility = View.VISIBLE
//                }
//
//                ViewState.NetworkStatus.SUCCESS -> {
//                    progress.visibility = View.GONE
//                    dialog.dismiss()
//                }
//
//                ViewState.NetworkStatus.ERROR -> {
//                    progress.visibility = View.GONE
//                    connectBtnEnable(dialog)
//                }
//
//                else -> {}
//            }
//        }
//    }

    private fun connectBtnEnable(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
    }

    private var progressDialog: AlertDialog? = null

    private fun showProgress(message: String = "Loading...") {
        if (progressDialog?.isShowing == true) return

        progressDialog = AlertDialog.Builder(this)
            .setView(ProgressBar(this))
            .setCancelable(false)
            .setMessage(message)
            .create()

        progressDialog?.show()
    }

    private fun hideProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun ensureTractorMasterLoaded() {
        lifecycleScope.launch {
            try {
                showProgress("Loading tractor models...")
                val response = tractorRepo.fetchIfNeeded()

                Timber.d("Inside EOL_TRACTOR", "API status=${response.status}")
                Timber.d("Inside EOL_TRACTOR", "API data size=${response.data?.size}")

                if (response.status && !response.data.isNullOrEmpty()) {
                    TractorMasterCache.save(response.data!!)
                } else {
                    Timber.e("EOL_TRACTOR", "Empty tractor master data")
                }
            } catch (e: Exception) {
                Timber.e("Failed to load list : ${e.message}")
                ViewUtils.showSnackbar(
                    binding.root,
                    "Failed to load tractor master data",
                    false
                )
            } finally {
                hideProgress()
            }
        }
    }

    private fun loadTractorMasterAndProceed() {

        if (TractorMasterCache.isAvailable()) {
            startActivity(Intent(this, EndOfLineTestingActivity::class.java))
            return
        }

        lifecycleScope.launch {
            try {
                loadingDialog.show("Loading tractor master...")

                val response = tractorRepo.fetchIfNeeded()

                Timber.d("Response : ${response.toString()}")
                if (response.status && !response.data.isNullOrEmpty()) {
                    TractorMasterCache.save(response.data)
                    Timber.d("EOL_TRACTOR", "Cache saved size=${TractorMasterCache.get().size}")
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Tractor master not available",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Intent(this@MainActivity, EndOfLineTestingActivity::class.java)

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load tractor master",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingDialog.dismiss()
            }
        }
    }

}

