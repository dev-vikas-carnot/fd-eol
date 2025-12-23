package com.carnot.fd.eol.features.activatesim

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.carnot.fd.eol.CustomDialog
import com.carnot.fd.eol.LoadingDialog
import com.carnot.fd.eol.R
import com.carnot.fd.eol.databinding.ActivitySimActivationBinding
import com.carnot.fd.eol.network.ApiResponse
import androidx.core.widget.doAfterTextChanged
import com.carnot.fd.eol.features.test.CameraXScannerActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SimActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimActivationBinding
    private lateinit var viewModel: SimActivationViewModel

    private lateinit var loadingDialog: LoadingDialog
    private lateinit var customDialog: CustomDialog

    private var isProgrammaticUpdate = false

    private val scanLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val scannedValue =
                result.data?.getStringExtra("SCAN_RESULT").orEmpty()

            if (scannedValue.isNotBlank()) {
                viewModel.setDeviceQRFromScan(scannedValue)
            }
        }

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

        // Scan QR to get IMEI
        binding.scanQR.setOnClickListener {
            hideKeyboard(binding.root)
            scanLauncher.launch(
                Intent(this, CameraXScannerActivity::class.java)
            )
        }

        // Manual IMEI entry - update ViewModel on each text change so submit button state updates
        binding.etImei.doAfterTextChanged {
            if (!isProgrammaticUpdate) {
                viewModel.setImeiManually(it?.toString().orEmpty())
            }
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

        viewModel.imei.observe(this) { imei ->
            isProgrammaticUpdate = true
            binding.etImei.setText(imei)
            binding.etImei.setSelection(imei.length)
            isProgrammaticUpdate = false
        }

        viewModel.iccid.observe(this) { iccid ->
            isProgrammaticUpdate = true
            binding.etIccid.setText(iccid)
            binding.etIccid.setSelection(iccid.length)
            isProgrammaticUpdate = false
        }
        binding.etIccid.doAfterTextChanged {
            if (!isProgrammaticUpdate) {
                viewModel.setIccidManually(it?.toString().orEmpty())
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


