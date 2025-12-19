package com.carnot.fd.eol.features.printer

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.carnot.fd.eol.R
import com.carnot.fd.eol.data.ViewState
import com.carnot.fd.eol.utils.Globals.isValidIp
import com.carnot.fd.eol.utils.PrinterPrefs
import com.carnot.fd.eol.utils.PrinterTestStatus
import androidx.fragment.app.activityViewModels
import com.carnot.fd.eol.utils.ViewUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrinterIpDialogFragment : DialogFragment() {

    private lateinit var etIp: EditText   // ✅ class-level
    private val printerViewModel: PrinterViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_printer_ip, null)
        etIp = view.findViewById(R.id.etPrinterIp)   // ✅ assign class property

        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val statusText = TextView(requireContext()).apply {
            textSize = 12f
        }

        (view as LinearLayout).addView(statusText)

        // 🔹 Prefill last IP
        etIp.setText(PrinterPrefs.getLastIp(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Test Zebra Printer")
            .setView(view)
            .setPositiveButton("CONNECT", null)
            .setNegativeButton("CANCEL") { _, _ ->
                dismiss()
                ViewUtils.hideKeyboard(etIp)
            }
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            btn.setOnClickListener {
                val ip = etIp.text.toString().trim()

                if (!isValidIp(ip)) {
                    etIp.error = "Invalid IP address"
                    return@setOnClickListener
                }

                btn.isEnabled = false
                progress.visibility = View.VISIBLE

                printerViewModel.testPrinterWithIp(ip)
            }
        }

        observePrinterStatus(dialog, progress, statusText)
        return dialog
    }

    private fun observePrinterStatus(
        dialog: AlertDialog,
        progress: ProgressBar,
        statusText: TextView
    ) {
        printerViewModel.printerTestStatus.observe(this) { state ->
            when (state) {
                PrinterTestStatus.Connecting -> {
                    statusText.text = "Connecting to printer…"
                }

                PrinterTestStatus.Ready -> {
                    statusText.text = "Printer ready"
                }

                PrinterTestStatus.Printing -> {
                    statusText.text = "Printing label…"
                }

                is PrinterTestStatus.Error -> {
                    progress.visibility = View.GONE
                    statusText.text = "Error: ${state.message}"
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true
                }

                else -> {}
            }
        }

        printerViewModel.printState.observe(this) {
            if (it.status == ViewState.NetworkStatus.SUCCESS) {
                val ip = etIp.text.toString().trim()
//                PrinterPrefs.saveLastIp(requireContext(), ip)
                printerViewModel.updatePrinterIp(ip)
                progress.visibility = View.GONE
                dismiss()
            }
        }
    }
}
