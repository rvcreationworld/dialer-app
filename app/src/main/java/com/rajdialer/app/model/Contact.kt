package com.rajdialer.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class Contact(
    val id: String,
    val name: String,
    val number: String,
    val photoUri: String? = null,
    val isFavorite: Boolean = false
) {
    val avatarInitials: String
        get() = name.split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }
}
