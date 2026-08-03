package com.rajdialer.app.core

import androidx.compose.runtime.Immutable

@Immutable
data class CallUiState(
    val callId: String = "",
    val number: String = "",
    val name: String? = null,
    val state: CallState = CallState.IDLE,
    val direction: CallDirection = CallDirection.UNKNOWN,
    val connectedTimestamp: Long = 0L,
    val supportedCapabilities: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false
) {
    val avatarInitials: String
        get() = (name ?: "Unknown").split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }
}
