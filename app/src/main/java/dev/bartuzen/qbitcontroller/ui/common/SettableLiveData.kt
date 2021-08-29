package dev.bartuzen.qbitcontroller.ui.common

import androidx.lifecycle.MutableLiveData

class SettableLiveData<T> : MutableLiveData<T>() {
    var isSet = false

    override fun setValue(value: T?) {
        super.setValue(value)
        isSet = true
    }
}