package com.rajdialer.app.core

import androidx.compose.runtime.Immutable

@Immutable
data class DialerState(
    val activeCall: CallState = CallState.IDLE,
    val calls: List<CallUiState> = emptyList()
)
