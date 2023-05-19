package dev.bartuzen.qbitcontroller.ui.lock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {
    private val _pin = MutableStateFlow("")
    val pin = _pin.asStateFlow()

    fun addDigit(digit: Int) {
        _pin.value += digit
    }

    fun deleteLastDigit() {
        _pin.value = pin.value.dropLast(1)
    }

    fun isPinCorrect() = pin.value == settingsManager.pin.value

    fun resetPin() {
        _pin.value = ""
    }
}
