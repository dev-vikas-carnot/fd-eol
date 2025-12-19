package com.carnot.fd.eol

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoadingDialog(private val context: Context) {

    private val dialog: Dialog = Dialog(context)
    private var messageTextView: TextView? = null

    init {
        // Inflate the dialog view
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        messageTextView = view.findViewById(R.id.loading_message)

        // Set up the dialog
        dialog.setContentView(view)
        dialog.setCancelable(false) // Make it non-cancelable
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    // Method to show the dialog
    fun show(message: String? = null) {
        message?.let {
            messageTextView?.text = it
        }
        dialog.show()
    }

    // Method to dismiss the dialog
    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}
