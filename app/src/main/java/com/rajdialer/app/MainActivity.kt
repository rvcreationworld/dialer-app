package com.rajdialer.app

import androidx.activity.enableEdgeToEdge
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rajdialer.app.telecom.DefaultDialerManager
import com.rajdialer.app.ui.AppRoot
import com.rajdialer.app.ui.theme.RajDialerTheme
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import kotlinx.coroutines.launch
import com.rajdialer.app.data.network.UpdaterService
import com.rajdialer.app.utils.ApkInstaller

class MainActivity : ComponentActivity() {
    
    private val defaultDialerManager by lazy { DefaultDialerManager(this) }
    
    // Launcher to handle the result of the default dialer request
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("RajDialer", "Successfully set as default dialer")
        } else {
            Log.w("RajDialer", "User declined to set as default dialer")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Custom Crash Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val sw = java.io.StringWriter()
            exception.printStackTrace(java.io.PrintWriter(sw))
            val stackTrace = sw.toString()
            getSharedPreferences("crash_prefs", MODE_PRIVATE).edit().putString("last_crash", stackTrace).commit()
            defaultHandler?.uncaughtException(thread, exception)
        }

        enableEdgeToEdge()
        
        checkAndRequestDefaultDialer()

        setContent {
            var updateUrl by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            
            LaunchedEffect(Unit) {
                scope.launch {
                    val update = UpdaterService.checkForUpdates(BuildConfig.VERSION_CODE)
                    if (update != null) {
                        updateUrl = update.second
                    }
                }
            }

            RajDialerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (updateUrl != null) {
                        AlertDialog(
                            onDismissRequest = { /* Must not dismiss */ },
                            title = { Text("Update Required") },
                            text = { Text("A new version of CallPulse is available. You must update to continue using the app.") },
                            confirmButton = {
                                Button(onClick = {
                                    ApkInstaller.downloadAndInstall(this@MainActivity, updateUrl!!)
                                }) {
                                    Text("Download & Update")
                                }
                            },
                            dismissButton = null
                        )
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }

    private fun checkAndRequestDefaultDialer() {
        if (!defaultDialerManager.isDefaultDialer()) {
            defaultDialerManager.getDefaultDialerIntent()?.let { intent ->
                requestRoleLauncher.launch(intent)
            }
        } else {
            Log.d("RajDialer", "App is already the default dialer")
        }
    }
}
