package com.rajdialer.app.ui.contacts

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajdialer.app.data.repository.ContactsRepository
import com.rajdialer.app.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Immutable
data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val filteredContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val permissionDenied: Boolean = false
)

class ContactsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState(isLoading = true))
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    fun loadContacts(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, permissionDenied = false)
            
            val repository = ContactsRepository(context)
            val contacts = repository.getContacts()
            
            _uiState.value = _uiState.value.copy(
                contacts = contacts,
                filteredContacts = contacts.filter { it.name.contains(_uiState.value.searchQuery, ignoreCase = true) },
                isLoading = false
            )
        }
    }

    private val _searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    val filtered = if (query.isBlank()) {
                        _uiState.value.contacts
                    } else {
                        _uiState.value.contacts.filter { 
                            it.name.contains(query, ignoreCase = true) || it.number.contains(query)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        searchQuery = query,
                        filteredContacts = filtered
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQueryFlow.value = query
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            permissionDenied = true,
            isLoading = false,
            contacts = emptyList(),
            filteredContacts = emptyList()
        )
    }
}
