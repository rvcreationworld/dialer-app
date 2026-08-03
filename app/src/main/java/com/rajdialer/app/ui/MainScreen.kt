package com.rajdialer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rajdialer.app.ui.contacts.ContactsScreen
import com.rajdialer.app.ui.keypad.KeypadScreen
import com.rajdialer.app.ui.recents.RecentsScreen
import com.rajdialer.app.ui.tasks.MyTasksScreen

enum class Screen {
    RECENTS, KEYPAD, CONTACTS, TASKS
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "callpulse_ss",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }
        },
        bottomBar = {
            DialerBottomNavigation(navController = navController)
        },
        containerColor = Color.White
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.RECENTS.name,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(Screen.RECENTS.name) { RecentsScreen() }
            composable(Screen.KEYPAD.name) { KeypadScreen() }
            composable(Screen.CONTACTS.name) { ContactsScreen() }
            composable(Screen.TASKS.name) { MyTasksScreen() }
        }
    }
}

@Composable
fun DialerBottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 12.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavIconItem(
            icon = Icons.Default.Phone,
            label = "Recents",
            isSelected = currentRoute == Screen.RECENTS.name,
            onClick = {
                if (currentRoute != Screen.RECENTS.name) {
                    navController.navigate(Screen.RECENTS.name) {
                        popUpTo(Screen.RECENTS.name) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavIconItem(
            icon = Icons.Default.Contacts,
            label = "Contacts",
            isSelected = currentRoute == Screen.CONTACTS.name,
            onClick = {
                if (currentRoute != Screen.CONTACTS.name) {
                    navController.navigate(Screen.CONTACTS.name) {
                        popUpTo(Screen.RECENTS.name) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavIconItem(
            icon = Icons.Default.Dialpad,
            label = "Keypad",
            isSelected = currentRoute == Screen.KEYPAD.name,
            onClick = {
                if (currentRoute != Screen.KEYPAD.name) {
                    navController.navigate(Screen.KEYPAD.name) {
                        popUpTo(Screen.RECENTS.name) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavIconItem(
            icon = Icons.Default.Assignment,
            label = "Tasks",
            isSelected = currentRoute == Screen.TASKS.name,
            onClick = {
                if (currentRoute != Screen.TASKS.name) {
                    navController.navigate(Screen.TASKS.name) {
                        popUpTo(Screen.RECENTS.name) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}

@Composable
fun NavIconItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent), // Light blue for selected
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color(0xFF1976D2) else Color(0xFF5F6368), // Primary blue vs Muted Grey
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (isSelected) Color(0xFF1976D2) else Color(0xFF5F6368)
        )
    }
}

