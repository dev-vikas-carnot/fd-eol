package com.carnot.fd.eol.features.login.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.carnot.fd.eol.BuildConfig
import com.carnot.fd.eol.MainActivity
import com.carnot.fd.eol.R
import com.carnot.fd.eol.databinding.ActivityLoginBinding
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_APP_OPENED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_OTP_API_FAILURE
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_OTP_API_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_OTP_ENTERED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_OTP_SCREEN_VIEWED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SIGNIN_API_CALLED
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SIGNIN_API_FAILURE
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_SIGNIN_API_SUCCESS
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_API
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_CLICK
import com.carnot.fd.eol.firebase.AnalyticsEvents.EVENT_TYPE_VIEW
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_LOGIN
import com.carnot.fd.eol.firebase.AnalyticsEvents.SCREEN_LOGIN_OTP
import com.carnot.fd.eol.firebase.FirebaseAnalyticsEvents
import com.carnot.fd.eol.network.NetworkResult
import com.carnot.fd.eol.utils.FullScreenUtils
import com.carnot.fd.eol.utils.Globals
import com.carnot.fd.eol.utils.PreferenceUtil
import com.carnot.fd.eol.utils.ViewUtils
import com.chaos.view.PinView
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dialog: MaterialDialog
    private lateinit var customView: View
    private lateinit var viewModel: LoginViewModel
    private var progressSnackbar: Snackbar? = null

    companion object {
        const val OTP_SIZE = 4
        const val MOBILE_NO_SIZE = 10
        const val baseTime: Long = 120000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceUtil.initPreference(this@LoginActivity)

        if ( PreferenceUtil.isUserLoggedIn){
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }else{
            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_VIEW)
            }
            FirebaseAnalyticsEvents.logEvent(EVENT_APP_OPENED, SCREEN_LOGIN,bundle)


            binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
           viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
           setUpUi()
          observeChanges()
        }
    }

    private fun setUpUi() {

        val appVersion = BuildConfig.VERSION_NAME
        val appVersionCode = BuildConfig.VERSION_CODE
        val buildType = BuildConfig.BUILD_TYPE
        val packageManager = packageManager
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val installTime = packageInfo.firstInstallTime // Returns milliseconds since epoch
        val installDate = Date(installTime)

        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(installDate)
        binding.versionTv.text =  "$buildType\nVersion: $appVersion ($appVersionCode)\nInstalled On: $formattedDate"


        handleWindowInsets()
//        window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)


        dialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).apply {
            cornerRadius(res = com.intuit.sdp.R.dimen._15sdp)
            customView(R.layout.login_bottom_sheet)
        }



        binding.etUserName.doAfterTextChanged {
            if (it?.trim()?.isNotEmpty() == true) {
//                binding.btnReqVerification.isEnabled = true
                viewModel.userName = it.trim().toString()

                val bundle = Bundle().apply {
                    putString("event_type", EVENT_TYPE_VIEW)
                    putString("user_name", it.toString())
                }
                FirebaseAnalyticsEvents.logEvent(EVENT_OTP_SCREEN_VIEWED,SCREEN_LOGIN_OTP,bundle)

                setUpBottomSheet()
            }
//            else {
//                binding.btnReqVerification.isEnabled = false
//            }
        }

        binding.tvPrivacy.setOnClickListener {
            //showTermsAndConditionDialog(this@LoginActivity)
        }

        binding.btnReqVerification.setOnClickListener {
            //Commented this block to go ahead to dashboard
            //Uncomment this once login api is shared
            if (binding.etUserName.text?.isNotEmpty() == true) {
            val bundle = Bundle().apply {
                putString("user_name", binding.etUserName.text.toString())
                putString("event_type", EVENT_TYPE_CLICK)
            }
            FirebaseAnalyticsEvents.logEvent(EVENT_SIGNIN_API_CALLED,SCREEN_LOGIN,bundle)
//            viewModel.login()
                dialog.show()
            ViewUtils.hideKeyboard(binding.btnReqVerification)
        }
//            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }
    }

    private fun observeChanges() {
        viewModel.loginState.observe(this) {
            when (it) {
                is NetworkResult.Loading -> {
                    progressSnackbar = ViewUtils.getSnackbarWithProgressIndicator(
                        binding.root,
                        this,
                        getString(R.string.loading_data)
                    )
                    progressSnackbar?.show()
                }
                is NetworkResult.Error -> {
                    ViewUtils.showSnackbar(binding.root, it.message, false)

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("user_name", binding.etUserName.text.toString())
                        putString("message", it.message.toString())
                    }
                    FirebaseAnalyticsEvents.logEvent(EVENT_SIGNIN_API_FAILURE,SCREEN_LOGIN,bundle)

                    progressSnackbar?.dismiss()
                }

                is NetworkResult.Success -> {

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("user_name", binding.etUserName.text.toString())
                        putString("message", it.message.toString())
                    }
                    FirebaseAnalyticsEvents.logEvent(EVENT_SIGNIN_API_SUCCESS,SCREEN_LOGIN,bundle)


                    progressSnackbar?.dismiss()
                    //viewModel.displayTimer(baseTime)
                    dialog.show()
                }
            }
        }

        viewModel.otpState.observe(this) {
            when (it) {
                is NetworkResult.Loading -> {
                    progressSnackbar = ViewUtils.getSnackbarWithProgressIndicator(
                        dialog.getCustomView(),
                        this,
                        getString(R.string.loading_data)
                    )
                    progressSnackbar?.show()
                }
                is NetworkResult.Error -> {
                    ViewUtils.showSnackbar(dialog.getCustomView(), it.message, false)

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("user_name", it.toString())
                        putString("message", it.message.toString())

                    }
                    FirebaseAnalyticsEvents.logEvent(EVENT_OTP_API_FAILURE,SCREEN_LOGIN_OTP,bundle)

                    progressSnackbar?.dismiss()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                }

                is NetworkResult.Success -> {

                    val bundle = Bundle().apply {
                        putString("event_type", EVENT_TYPE_API)
                        putString("user_name", it.toString())
                        putString("message", it.message.toString())
                    }
                    FirebaseAnalyticsEvents.logEvent(EVENT_OTP_API_SUCCESS,SCREEN_LOGIN_OTP,bundle)

                    dialog.dismiss()
                    progressSnackbar?.dismiss()
                    PreferenceUtil.isUserLoggedIn = true
                    PreferenceUtil.userName = viewModel.userName ?: ""
                    PreferenceUtil.userId = it.data?.id?.toString() ?: ""
                    Globals.setJWTAccessToken(this,it.data?.authTokens?.accessToken.toString())
                    Globals.setJWTRefreshToken(this,it.data?.authTokens?.refreshToken.toString())
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun handleWindowInsets() {

        FullScreenUtils.hideSystemNavigationUI(window, binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sysWindow = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                v.updatePadding(bottom = sysWindow.bottom)
            }
            insets
        }
    }



    private fun resendOTP(view: TextView) {
        if (view.isEnabled) {
            view.isEnabled = false
            viewModel.login()
            viewModel.displayTimer(baseTime)
            view.setText(R.string.retry)
        }
    }


    private fun setUpBottomSheet() {
        customView = dialog.getCustomView()
        val btn = customView.findViewById<Button>(R.id.btn_submit)
        val no = customView.findViewById<TextView>(R.id.tv_username)
        val timer = customView.findViewById<TextView>(R.id.timer)
        val resendOTP = customView.findViewById<TextView>(R.id.resend_otp)
        val otp = customView.findViewById<PinView>(R.id.pinView)
        val ivEdit = customView.findViewById<ImageView>(R.id.iv_edit)


        ivEdit.setOnClickListener {
            dialog.dismiss()
        }

        no.text = viewModel.userName
        otp.doAfterTextChanged {
            if (it?.trim()?.length == OTP_SIZE) {
                btn.isEnabled = true
                btn.setTextColor(getColor(R.color.white))
                btn.setBackgroundColor(getColor(R.color.colorPrimary))
                viewModel.password = it.trim().toString()
            }else{
                btn.isEnabled = false
            }
//            btn.isEnabled = it?.length == OTP_SIZE
        }

        resendOTP.setOnClickListener {
            resendOTP(resendOTP)
        }

        btn.setOnClickListener {
            ViewUtils.hideKeyboard(it)

            val bundle = Bundle().apply {
                putString("event_type", EVENT_TYPE_CLICK)
                putString("user_name", it.toString())
            }
            FirebaseAnalyticsEvents.logEvent(EVENT_OTP_ENTERED,SCREEN_LOGIN_OTP,bundle)


            viewModel.verifyOTP()
        }

        viewModel.getTimerResponse.observe(
            this
        ) { apiResponse ->

            when (apiResponse.message) {

                "timer" -> {
                    timer.visibility = View.VISIBLE
                    resendOTP.isEnabled = false
                    timer.text = apiResponse.data + " " + getString(R.string.secs)
                }
                "success" -> {
                    timer.visibility = View.GONE
                    resendOTP.text = getString(R.string.resend_otp_heading)
                    resendOTP.isEnabled = true
                }
            }
        }


    }


    /*
    fun showTermsAndConditionDialog(context: Context) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        val legalDocView: View = View.inflate(context, R.layout.dialog_legal_doc, null)
        val tvTermsCondition: TextView = legalDocView.findViewById(R.id.tv_terms_condition)
        val inputStream: InputStream =
            context.resources.openRawResource(R.raw.operator_policy)
        val buffer: ByteArray
        try {
            buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            tvTermsCondition.text = Html.fromHtml(String(buffer))
            builder.setView(legalDocView)
            val dialog: AlertDialog = builder.create()
            if (dialog.window != null) {
                dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
                dialog.setCanceledOnTouchOutside(true)
                dialog.window!!.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            dialog.show()
            val ivTermsConditionsClose: ImageView =
                legalDocView.findViewById(R.id.iv_close_term_and_condition)
            ivTermsConditionsClose.setOnClickListener { dialog.dismiss() }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

     */
}