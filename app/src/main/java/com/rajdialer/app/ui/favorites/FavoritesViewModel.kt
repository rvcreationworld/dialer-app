package com.rajdialer.app.ui.favorites

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.rajdialer.app.model.Contact
import com.rajdialer.app.model.DummyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class FavoritesUiState(
    val favorites: List<Contact> = DummyData.favorites,
    val isLoading: Boolean = false
)

class FavoritesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
}
