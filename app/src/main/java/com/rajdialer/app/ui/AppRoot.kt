package com.rajdialer.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rajdialer.app.data.preferences.AppPreferences
import com.rajdialer.app.ui.auth.ConnectUrlScreen
import com.rajdialer.app.ui.auth.LoginScreen

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    val navController = rememberNavController()

    val crashPrefs = context.getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
    var lastCrash by remember { mutableStateOf(crashPrefs.getString("last_crash", null)) }

    if (lastCrash != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { androidx.compose.material3.Text("App Crashed!") },
            text = { 
                androidx.compose.foundation.lazy.LazyColumn {
                    item { androidx.compose.material3.Text(lastCrash!!) }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { 
                    crashPrefs.edit().remove("last_crash").apply()
                    lastCrash = null
                }) {
                    androidx.compose.material3.Text("Clear & Continue")
                }
            }
        )
        return // DO NOT RENDER THE REST OF THE APP IF IT CRASHED
    }

    val startDestination = when {
        prefs.baseUrl.isEmpty() -> "connect_url"
        prefs.jwtToken.isEmpty() -> "login"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("connect_url") {
            ConnectUrlScreen(
                onUrlSaved = {
                    if (prefs.jwtToken.isEmpty()) {
                        navController.navigate("login") {
                            popUpTo("connect_url") { inclusive = true }
                        }
                    } else {
                        navController.navigate("main") {
                            popUpTo("connect_url") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                onLogout = {
                    prefs.clearAuth()
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
