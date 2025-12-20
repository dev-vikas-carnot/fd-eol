package com.carnot.fd.eol.firebase


object AnalyticsEvents {

    const val SCREEN_HOME = "MainActivity"
    const val SCREEN_LOGIN_OTP = "LoginOTPScreen"
    const val SCREEN_LOGIN = "LoginScreen"
    const val SCREEN_EOL = "EOLScreen"
    const val SCREEN_REPRINT = "ReprintScreen"
    const val SCREEN_staTus = "VerifyStatusScreen"

    const val SCREEN_FAQ = "FAQScreen"

    const val EVENT_TYPE_CLICK = "click_event"
    const val EVENT_TYPE_VIEW = "view_event"
    const val EVENT_TYPE_API = "api_interaction_event"
    const val EVENT_TYPE_BACKEND = "error_triggered"


    const val EVENT_APP_OPENED = "app_opened"
    const val EVENT_SIGNIN_API_CALLED = "signin_api_called"
    const val EVENT_SIGNIN_API_SUCCESS = "signin_api_success"
    const val EVENT_SIGNIN_API_FAILURE = "signin_api_failure"
    const val EVENT_OTP_SCREEN_VIEWED = "otp_screen_viewed"
    const val EVENT_OTP_ENTERED = "otp_entered"
    const val EVENT_OTP_API_SUCCESS = "otp_api_success"
    const val EVENT_OTP_API_FAILURE = "otp_api_failure"
    const val EVENT_USER_LOGOUT = "user_logout"

    const val EVENT_HOME_SCREEN_VIEWED = "home_screen_viewed"
    const val EVENT_SIM_ACTIVATION_CLICKED = "activate_sim_clicked"
    const val EVENT_LINK_DEVICE_AND_TRACTOR_CLICKED = "link_device_and_tractor_clicked"
    const val EVENT_END_OF_LINE_TESTING_CLICKED = "end_of_line_testing_clicked"
    const val EVENT_TEST_PRINTER_CLICKED = "test_printer_clicked"
    const val EVENT_REPRINT_CLICKED = "reprint_clicked"
    const val EVENT_SHARE_LOG_FILE_CLICKED = "share_log_file_clicked"
    const val EVENT_STATUSCHK_CLICKED = "status_check_clicked"


    const val EVENT_LINK_SCREEN_VIEWED = "link_screen_viewed"
    const val EVENT_SCAN_DEVICE_QR_CLICKED = "scan_device_qr_clicked"
    const val EVENT_IMEI_ICCID_SCAN_SUCCESS = "imei_iccid_scan_success"

    const val EVENT_Step1_LDT= "link_devicetractor_step1_success"
    const val EVENT_Step2_LDT= "link_devicetractor_step2_success"
    const val EVENT_Submit_LDT= "link_devicetractor_submit_success"

    const val EVENT_Step1_EOL= "eol_step1_success"
    const val EVENT_Step2_EOL= "eol_step2_success"
    const val EVENT_Submit_EOL= "eol_submit_success"



    const val EVENT_IMEI_ICCID_SCAN_FAILURE = "imei_iccid_scan_failure"
    const val EVENT_IMEI_ICCID_SCAN_CANCEL = "imei_iccid_scan_cancelled"

    const val EVENT_SCAN_VIN_CLICKED = "scan_vin_clicked"
    const val EVENT_SCAN_VIN_CANCELLED = "scan_vin_cancel"

    const val EVENT_SCAN_VIN_SUCCESS = "scan_vin_success"
    const val EVENT_SCAN_VIN_FAILURE = "scan_vin_failure"

    const val EVENT_LINK_DEVICE_CLICKED = "link_device_clicked"
    const val EVENT_LINK_DEVICE_SUCCESS = "link_device_success"

    const val EVENT_QR_VALIDATION_FAILED = "qr_validation_failed"//Added
    const val EVENT_QR_PARSE_ERROR = "qr_parse_error"//Added
    const val EVENT_LINK_DEVICE_FAILURE = "link_device_failure"
    const val EVENT_LINK_DEVICE_CANCELLED = "link_device_cancelled"


    const val EVENT_EOL_SCREEN_VIEWED = "eol_screen_viewed"
    const val EVENT_SCAN_VIN2_CLICKED = "scan_vin2_clicked"
    const val EVENT_SCAN_IMEI2_CLICKED = "scan_imei2_clicked"
    const val EVENT_SCAN_VIN2_SUCCESS = "scan_vin2_success"
    const val EVENT_SCAN_VIN2_FAILURE = "scan_vin2_failure"
    const val EVENT_SCAN_IMEI2_FAILURE = "scan_imei2_failure"
    const val EVENT_SCAN_VIN2_CANCELLED = "scan_vin2_cancel"
    const val EVENT_SCAN_IMEI2_CANCELLED = "scan_imei2_cancel"

    const val EVENT_FLAGS_API_CALLED = "flags_api_called"
    const val EVENT_FLAGS_API_SUCCESS = "flags_api_success"
    const val EVENT_FLAGS_API_FAILURE = "flags_api_failure"
    const val EVENT_RETRY_CLICKED = "retry_clicked"
    const val EVENT_EOL_API_CALLED = "eol_api_called"
    const val EVENT_EOL_API_SUCCESS = "eol_api_success"
    const val EVENT_EOL_API_FAILURE = "eol_api_failure"

    const val EVENT_PRINT_BUTTON_CLICKED = "print_button_click"


    const val EVENT_MANUAL_ENTRY = "MANUAL_ENTRY"

    const val EVENT_TEST_PRINTER_SCREEN_VIEWED = "test_printer_screen_viewed"
    const val EVENT_PRINTER_TEST_STARTED = "printer_test_started"
    const val EVENT_PRINTER_TEST_SUCCESS = "printer_test_success"
    const val EVENT_MODULE_ALREADY_INSTALLED = "mod_already_installed"
    const val EVENT_MODULE_INSTALLED_SUCCESS = "mod_installed_success"
    const val EVENT_MODULE_INSTALL_ERROR = "mod_install_error"
    const val EVENT_PRINTER_TEST_FAILURE = "printer_test_failure"

    const val EVENT_REPRINT_SCREEN_VIEWED = "reprint_screen_viewed"
    const val EVENT_REPRINT_SCAN = "reprint_scan_start"
    const val EVENT_REPRINT_SCANSUCCESS = "reprint_scan_success"
    const val EVENT_REPRINT_SCANFAIL = "reprint_scan_failure"
    const val EVENT_REPRINT_SCANCANCELL = "reprint_scan_cancel"


    const val EVENT_REPRINT_SCAN_CLICKED = "reprint_scan_clicked"
    const val EVENT_REPRINT_SCAN_SUCCESS = "reprint_scan_success"
    const val EVENT_REPRINT_SCAN_FAILURE = "reprint_scan_failure"

    //    const val EVENT_SHARE_LOG_FILE_SCREEN_VIEWED = "share_log_file_screen_viewed"
//    const val EVENT_LOG_FILE_SHARED = "log_file_shared"
//    const val EVENT_LOG_FILE_SHARE_FAILURE = "log_file_share_failure"
    const val EVENT_FAQ_VIEWED = "faq_viewed"

    const val EVENT_SHARE_LOG_FILE_STARTED = "share_log_file_started"
    const val EVENT_SHARE_LOG_FILE_NOT_FOUND = "share_log_file_not_found"
    const val EVENT_SHARE_LOG_FILE_DIALOG_SHOWN = "share_log_file_dialog_shown"
    const val EVENT_SHARE_LOG_FILE_ERROR = "share_log_file_error"

    const val EVENT_NETWORK_APICALL = "network_call"
    const val EVENT_NETWORK_ERROR = "network_error"
    const val EVENT_NETWORK_API_RESPONSE = "network_response" // ✅ Added for `logApiResponse`

    // API Names
    const val API_LOGIN_OTP = "login_otp_call"
    const val API_LOGIN_VERIFY = "login_verify_call"
    const val API_SD_DEVICE_CREATION = "sd_device_creation_call"
    const val API_SD_DEVICE_INSTALLATION_STATUS = "sd_device_installation_status_call"
    const val API_SD_DEVICE_POST_INSTALLATION_TEST = "sd_device_post_installation_test_call"
    const val API_SD_DEVICE_POST_INSTALLATION_STATUS_PRINT ="sd_device_post_installation_status_print_call"


    // Common Bundle Keys
    const val BUNDLE_KEY_API_NAME = "api_name"
    const val BUNDLE_KEY_NAME = "name"
    const val BUNDLE_KEY_PARAMS = "params"
    const val BUNDLE_KEY_RESPONSE = "response"
    const val BUNDLE_KEY_ERROR = "error"
    const val BUNDLE_KEY_MESSAGE = "message"


}
