package com.rajdialer.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class CallHistory(
    val id: String,
    val name: String,
    val number: String,
    val time: String,
    val dateGroup: String,
    val timeOnly: String,
    val timestamp: Long,
    val duration: String,
    val durationSeconds: Long = 0,
    val type: CallType
) {
    val avatarInitials: String
        get() = name.split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }
}

enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED
}
