package com.carnot.fd.eol

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class CustomDialog(private val activity: Activity) {

    private val dialog: Dialog = Dialog(activity)


    fun show(icon:Int,title:String,message: String, isCancellable: Boolean = false,shouldShowPrint:Boolean = false,onPrintClicked:(()->Unit)? = null) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_success, null)

        // Find the views in the custom dialog layout
        val closeButton: ImageView = view.findViewById(R.id.closeButton)
        val dialogMessage: TextView = view.findViewById(R.id.dialogMessage)
        val dialogTitle : TextView = view.findViewById(R.id.dialogTitle)
        val dialogIcon :ImageView= view.findViewById(R.id.dialogIcon)
        val printButton: Button = view.findViewById(R.id.btnTestZebraPrinter)

        dialogTitle.text = title

        dialogIcon.setImageResource(icon)

        // Set the custom message
        if (message.isNullOrEmpty()){
            dialogMessage.visibility = View.GONE
        }
        dialogMessage.text = message

        // Set a click listener for the close button
        closeButton.setOnClickListener {
            dialog.dismiss()
            activity.finish()
        }
        printButton.setOnClickListener {
            onPrintClicked?.invoke()
        }

        if(shouldShowPrint){
            printButton.visibility = View.VISIBLE
        }

        // Set the custom view for the dialog
        dialog.setContentView(view)
        dialog.setCancelable(isCancellable)
        dialog.show()
    }

    fun showTwoAction(
        icon: Int,
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        onPositiveClick: () -> Unit,
        onNegativeClick: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setIcon(icon)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(positiveText) { dialog, _ ->
                dialog.dismiss()
                onPositiveClick()
            }
            .setNegativeButton(negativeText) { dialog, _ ->
                dialog.dismiss()
                onNegativeClick()
            }
            .show()
    }

    fun dismiss() {
        dialog.dismiss()
    }


}
