package com.carnot.fd.eol.features.vehicle_mapping.domain

object TractorMasterCache {

    private var cachedList: List<TractorModel>? = null

    fun isAvailable(): Boolean = !cachedList.isNullOrEmpty()

    fun save(list: List<TractorModel>) {
        cachedList = list
    }

    fun get(): List<TractorModel> = cachedList ?: emptyList()

    fun clear() {
        cachedList = null
    }
}
