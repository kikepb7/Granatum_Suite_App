package com.granatum.core.presentation.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

class ScopedStoreRegistryViewModel : ViewModel() {

    private val stores = mutableMapOf<String, ViewModelStore>()

    override fun onCleared() {
        super.onCleared()
        stores.values.forEach { it.clear() }
        stores.clear()
    }

    fun getOrCreate(id: String): ViewModelStore =
        stores.getOrPut(key = id) { ViewModelStore() }

    fun clear(id: String) = stores.remove(key = id)?.clear()
}