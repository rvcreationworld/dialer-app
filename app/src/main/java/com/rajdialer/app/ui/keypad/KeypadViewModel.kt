package com.rajdialer.app.ui.keypad

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajdialer.app.core.TelecomController
import com.rajdialer.app.data.repository.ContactsRepository
import com.rajdialer.app.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class KeypadUiState(
    val inputNumber: String = "",
    val isDialing: Boolean = false,
    val permissionDenied: Boolean = false,
    val suggestedContacts: List<Contact> = emptyList()
)

class KeypadViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(KeypadUiState())
    val uiState: StateFlow<KeypadUiState> = _uiState.asStateFlow()

    private var allContacts: List<Contact> = emptyList()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            val repository = ContactsRepository(getApplication())
            allContacts = repository.getContacts()
        }
    }

    fun appendNumber(num: String) {
        if (_uiState.value.isDialing) return
        val newNumber = _uiState.value.inputNumber + num
        updateSuggestions(newNumber)
    }

    fun backspace() {
        if (_uiState.value.isDialing) return
        val current = _uiState.value.inputNumber
        if (current.isNotEmpty()) {
            val newNumber = current.dropLast(1)
            updateSuggestions(newNumber)
        }
    }
    
    fun setNumber(num: String) {
        _uiState.value = _uiState.value.copy(inputNumber = num)
        updateSuggestions(num)
    }

    private fun updateSuggestions(number: String) {
        val suggestions = if (number.isBlank()) {
            emptyList()
        } else {
            allContacts.filter { 
                it.number.replace(Regex("[^0-9+]"), "").contains(number) 
            }.take(5)
        }
        _uiState.value = _uiState.value.copy(inputNumber = number, suggestedContacts = suggestions)
    }

    fun dial(context: Context) {
        val number = _uiState.value.inputNumber
        if (number.isBlank() || _uiState.value.isDialing) return

        Log.d("KeypadViewModel", "Dial Requested for number length: ${number.length}")
        _uiState.value = _uiState.value.copy(isDialing = true, permissionDenied = false)
        
        TelecomController.placeCall(context, number)
        
        // Clear UI and allow subsequent calls
        _uiState.value = _uiState.value.copy(inputNumber = "", isDialing = false, suggestedContacts = emptyList())
    }

    fun onPermissionDenied() {
        Log.w("KeypadViewModel", "Permission Denied registered in ViewModel")
        _uiState.value = _uiState.value.copy(permissionDenied = true, isDialing = false)
    }
    
    fun dismissPermissionError() {
        _uiState.value = _uiState.value.copy(permissionDenied = false)
    }
}
