package com.carnot.fd.eol.features.vehicle_mapping.domain

import com.carnot.fd.eol.utils.PreferenceUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber

object TractorMasterCache {

    private var cachedList: List<TractorModel>? = null
    private val gson = Gson()
    private const val KEY_TRACTOR_MASTER_LIST = "KEY_TRACTOR_MASTER_LIST"

    fun isAvailable(): Boolean {
        if (!cachedList.isNullOrEmpty()) return true

        return try {
            val json = PreferenceUtil.sharedPreferences.getString(KEY_TRACTOR_MASTER_LIST, null)
            !json.isNullOrEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Error checking TractorMasterCache availability")
            false
        }
    }

    fun save(list: List<TractorModel>) {
        if (cachedList === list) return
        cachedList = list
        try {
            val json = gson.toJson(list)
            PreferenceUtil.sharedPreferences.edit().putString(KEY_TRACTOR_MASTER_LIST, json).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error saving TractorMasterCache")
        }
    }

    fun get(): List<TractorModel> {
        if (!cachedList.isNullOrEmpty()) return cachedList!!

        try {
            val json = PreferenceUtil.sharedPreferences.getString(KEY_TRACTOR_MASTER_LIST, null)
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<TractorModel>>() {}.type
                cachedList = gson.fromJson(json, type)
                return cachedList ?: emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting TractorMasterCache")
        }

        return emptyList()
    }

    fun clear() {
        cachedList = null
        try {
            PreferenceUtil.sharedPreferences.edit().remove(KEY_TRACTOR_MASTER_LIST).apply()
        } catch (e: Exception) {
            Timber.e(e, "Error clearing TractorMasterCache")
        }
    }
}
