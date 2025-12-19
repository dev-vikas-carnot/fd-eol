package com.carnot.fd.eol.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import com.google.android.material.snackbar.Snackbar

object ViewUtils {

    fun hideKeyboard(view: View) {
        val inputMethodManager =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @JvmStatic
    fun showSnackbar(layout: View, message: String?, indefinite: Boolean) {
        var errorMessage = message
        if (errorMessage == null) {
            errorMessage = "Something went wrong"
        }
        if (indefinite) {
            val snackbar = Snackbar.make(layout, errorMessage, Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction("OK") { v: View -> snackbar.dismiss() }
            snackbar.show()
        } else {
            Snackbar.make(layout, errorMessage, Snackbar.LENGTH_LONG).show()
        }
    }

    fun getSnackbarWithProgressIndicator(
        container: View,
        context: Context,
        message: String?
    ): Snackbar? {
        val bar = Snackbar.make(container, message!!, Snackbar.LENGTH_INDEFINITE)
        val contentLay = bar.view.findViewById<View>(com.google.android.material.R.id.snackbar_text).parent as ViewGroup
        val item = ProgressBar(context)
        item.scaleY = 0.8f
        item.scaleX = 0.8f
        item.interpolator = AccelerateInterpolator()
        item.indeterminateDrawable.colorFilter =
            PorterDuffColorFilter(Color.parseColor("#652078"), PorterDuff.Mode.MULTIPLY)
        contentLay.addView(item)
        return bar
    }
}
