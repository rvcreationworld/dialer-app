package com.rajdialer.app.model

object DummyData {
    val contacts = listOf(
        Contact("1", "Alice Johnson", "+1 234 567 8901", null, true),
        Contact("2", "Bob Smith", "+1 234 567 8902", null, false),
        Contact("3", "Charlie Brown", "+1 234 567 8903", null, true),
        Contact("4", "Diana Prince", "+1 234 567 8904", null, false)
    )

    val favorites = contacts.filter { it.isFavorite }
}
