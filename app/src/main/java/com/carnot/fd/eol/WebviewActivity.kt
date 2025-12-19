package com.carnot.fd.eol


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.carnot.fd.eol.utils.Constants.metabaseURL
import java.net.URLEncoder

class WebviewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a layout to wrap the WebView
        val layout = FrameLayout(this)
        webView = WebView(this)

        layout.addView(
            webView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(layout)

        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.isVerticalScrollBarEnabled = true
        webView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
        webView.setInitialScale(1)

        // Receive data
        val vin = "" // Optional
        val status = "" // Optional
        val userId = intent.getStringExtra("user_id") ?: ""
        val userName = intent.getStringExtra("user_name") ?: ""
        val plantName = intent.getStringExtra("plant_name") ?: ""

        // Encode parameters
        val encodedVin = URLEncoder.encode(vin, "UTF-8")
        val encodedStatus = URLEncoder.encode(status, "UTF-8")
        val encodedUserId = URLEncoder.encode(userId, "UTF-8")
        val encodedUserName = URLEncoder.encode(userName, "UTF-8")
        val encodedPlantName = URLEncoder.encode(plantName, "UTF-8")

        val finalUrl = "https://metabase.mahindradigisense.com/public/question/6d704972-c889-4510-a0d6-e1edce630f1f"
//        val finalUrl = "$metabaseURL?plant_name=$encodedPlantName#hide_parameters=plant_name"
//        val finalUrl = "$baseUrl?vin=$encodedVin&status=$encodedStatus&user_id=$encodedUserId&user_name=$encodedUserName&plant_name=$encodedPlantName"

        Log.e("WEBVIEW", "FINAL URL -> $finalUrl")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript(
                    "document.body.style.overflow = 'auto'; document.body.style.height = 'auto';",
                    null
                )
            }
        }

        // Load URL
        webView.loadUrl(finalUrl)

     /*   webView.loadData(
            "<html><body><div style='height:2000px;background:linear-gradient(white,gray)'>Scroll Me</div></body></html>",
            "text/html",
            "UTF-8"
        )*/

    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
