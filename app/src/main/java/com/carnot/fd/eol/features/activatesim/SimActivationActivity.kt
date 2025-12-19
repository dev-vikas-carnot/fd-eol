package com.carnot.fd.eol.features.activatesim

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.carnot.fd.eol.CustomDialog
import com.carnot.fd.eol.LoadingDialog
import com.carnot.fd.eol.R
import com.carnot.fd.eol.databinding.ActivitySimActivationBinding
import com.carnot.fd.eol.network.ApiResponse
import androidx.core.widget.doAfterTextChanged
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.integration.android.IntentIntegrator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SimActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimActivationBinding
    private lateinit var viewModel: SimActivationViewModel

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var customDialog: CustomDialog

    /*Intent Handles For QR Code Scanning*/
    private var scanIntent: IntentIntegrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[SimActivationViewModel::class.java]
        binding = ActivitySimActivationBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.sim_activation_title)

        loadingDialog = LoadingDialog(this)
        customDialog = CustomDialog(this)

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC
            )
            .enableAutoZoom()
            .build()

        scanIntent = IntentIntegrator(this)
        val scanner = GmsBarcodeScanning.getClient(this, options)

        // Scan QR to get IMEI
        binding.scanQR.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.let { viewModel.setImei(it) }
                }
                .addOnCanceledListener {
                    // user cancelled scan; no-op
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.error_scan_failed), Toast.LENGTH_SHORT)
                        .show()
                }
        }

        // Manual IMEI entry - update ViewModel on each text change so submit button state updates
        binding.etImei.doAfterTextChanged {
            viewModel.setImei(it?.toString().orEmpty())
        }

        // Submit button
        binding.submitButton.setOnClickListener {
            viewModel.onSubmitClick()
        }

        // Observe API state
        viewModel.apiResponse.observe(this) { response ->
            when (response) {
                is ApiResponse.Loading -> {
                    loadingDialog.show(getString(R.string.sim_activation_progress))
                }

                is ApiResponse.Error -> {
                    loadingDialog.dismiss()
                    customDialog.show(
                        icon = R.drawable.baseline_error_outline_24,
                        title = getString(R.string.sim_activation_title),
                        message = response.message
                    )
                }

                is ApiResponse.Success -> {
                    loadingDialog.dismiss()
                    customDialog.show(
                        icon = R.drawable.baseline_check_circle_outline_24,
                        title = getString(R.string.sim_activation_title),
                        message = response.message ?: getString(R.string.sim_activation_success)
                    )
                }
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}


