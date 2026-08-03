package com.rajdialer.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.StarBorder

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Recents : BottomNavItem("recents", "Recents", Icons.Filled.Call, Icons.Outlined.Call)
    object Keypad : BottomNavItem("keypad", "Keypad", Icons.Filled.Dialpad, Icons.Outlined.Dialpad)
    object Contacts : BottomNavItem("contacts", "Contacts", Icons.Filled.Contacts, Icons.Outlined.Contacts)
    object Favorites : BottomNavItem("favorites", "Favorites", Icons.Filled.Star, Icons.Outlined.StarBorder)
}
