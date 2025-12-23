package com.carnot.fd.eol.utils

object Constants {
    const val metabaseURL ="https://metabase.fdtelematics.in/public/question/73e4419d-d1eb-4b58-b184-8abb7923ecc6";

    /**
     * TEMPORARY / TESTING ONLY:
     * When true, the EOL Device Status step is simulated on-device (GPS -> GSM -> Battery),
     * without calling backend polling APIs. Flip to false to restore the real polling flow.
     */
    const val EOL_DUMMY_STATUS_ENABLED: Boolean = false

    const val PREFERENCE_COM_CARNOT_EOL_APP = "com.carnot.fd.eol.app"
    const val PREFERENCE_MOBILE_NUMBER = "mobile_number"
    const val PREFERENCE_USER_NAME = "user_name"
    const val PREFERENCE_PLANT_ID = "plant_id"
    const val PREFERENCE_USER_LOGGEDIN = "preference_user_loggedin"
    const val PREFERENCE_SELECTED_LANGUAGE = "preference_language"
    const val PREFERENCE_USER_ID = "preference_user_id"
    const val COM_CARNOT_KRISHI_FIELD_USER_DETAILS = "com.carnot.fd.eol.USER_DETAILS"

    // Jwt tokens
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_IS_REFRESHTKNEXPIRY = "key_refresh_tkn"

    // DreamFactory API key (used for all external Mahindra APIs)
    const val DREAMFACTORY_API_KEY =
        "5fdf60e35d97fd70c6676050f64dac1d127ffec5f2260dd6d2607e6b231fd234"

    const val PLANT_1_KEY = "plant1"
    const val PLANT_2_KEY = "plant2"
    const val PLANT_3_KEY = "plant3"
    const val PLANT_DEFAULT_KEY = "DEFAULT_PLANT"

    const val PLANT_1_IP = "10.210.2.9"
    const val PLANT_2_IP = "10.210.4.56"
    const val PLANT_3_IP = "10.210.24.38"
    const val DEFAULT_IP = "0.0.0.0" // Fallback IP
    const val PRINTER_PORT = 9100



    const val PLANT_1_NAME = "Plant 1"
    const val PLANT_2_NAME = "Plant 2"
    const val PLANT_3_NAME = "Plant 3"


    const val ANDROID: String = "Android"

    const val IOS: String = "IOS"
}
