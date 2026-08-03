package com.rajdialer.app.ui.recents

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajdialer.app.data.repository.CallLogRepository
import com.rajdialer.app.model.CallHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Immutable
data class RecentsUiState(
    val callLogs: List<CallHistory> = emptyList(),
    val filteredLogs: List<CallHistory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val permissionDenied: Boolean = false
)

class RecentsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RecentsUiState(isLoading = true))
    val uiState: StateFlow<RecentsUiState> = _uiState.asStateFlow()

    fun loadCallLogs(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, permissionDenied = false)
            
            try {
                val repository = CallLogRepository(context)
                val logs = repository.getCallLogs()
                
                _uiState.value = _uiState.value.copy(
                    callLogs = logs,
                    filteredLogs = logs.filter { it.name.contains(_uiState.value.searchQuery, ignoreCase = true) || it.number.contains(_uiState.value.searchQuery) },
                    isLoading = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, permissionDenied = true)
            }
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
                        _uiState.value.callLogs
                    } else {
                        _uiState.value.callLogs.filter { 
                            it.name.contains(query, ignoreCase = true) || it.number.contains(query)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        searchQuery = query,
                        filteredLogs = filtered
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query) // Optimistic UI update for text field
        _searchQueryFlow.value = query
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            permissionDenied = true,
            isLoading = false,
            callLogs = emptyList(),
            filteredLogs = emptyList()
        )
    }
}
