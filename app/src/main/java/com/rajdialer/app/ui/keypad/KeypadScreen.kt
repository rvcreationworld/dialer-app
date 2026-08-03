package com.rajdialer.app.ui.keypad

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajdialer.app.ui.components.NumberButton
import com.rajdialer.app.ui.theme.BackgroundWhite
import com.rajdialer.app.ui.theme.CallGreen
import com.rajdialer.app.ui.theme.SurfaceWhite
import com.rajdialer.app.ui.theme.TextPrimary
import com.rajdialer.app.ui.theme.TextSecondary

@Composable
fun KeypadScreen(viewModel: KeypadViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        if (isGranted[Manifest.permission.CALL_PHONE] == true) {
            Log.d("KeypadScreen", "Permission Granted")
            viewModel.loadContacts()
            viewModel.dial(context)
        } else {
            Log.w("KeypadScreen", "Permission Denied")
            viewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        val hasContactsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasContactsPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
        }
    }

    LaunchedEffect(uiState.permissionDenied) {
        if (uiState.permissionDenied) {
            snackbarHostState.showSnackbar("CALL_PHONE permission is required to dial.")
            viewModel.dismissPermissionError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(bottom = 20.dp), // Reduce padding to move keypad down
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            
            // Display Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Suggestions List
                if (uiState.suggestedContacts.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(uiState.suggestedContacts) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.setNumber(contact.number.replace(Regex("[^0-9+]"), ""))
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE5E5EA)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = contact.name.split(" ")
                                        .filter { it.isNotBlank() }
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                                        .take(2).joinToString("")
                                    Text(text = initials.ifEmpty { "?" }, fontSize = 14.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = contact.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Text(text = contact.number, fontSize = 14.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Number Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.inputNumber,
                        fontSize = 40.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (uiState.inputNumber.isNotEmpty()) {
                        IconButton(onClick = { viewModel.backspace() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // Keypad Grid
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rows = listOf(
                    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                    listOf("*" to "", "0" to "+", "#" to "")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for ((num, letters) in row) {
                            NumberButton(
                                number = num,
                                letters = letters,
                                onClick = { viewModel.appendNumber(num) },
                                onLongClick = { if (num == "0") viewModel.appendNumber("+") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Call Button
            IconButton(
                onClick = {
                    if (uiState.inputNumber.isNotEmpty() && !uiState.isDialing) {
                        val hasPhonePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                        
                        if (hasPhonePermission) {
                            viewModel.dial(context)
                        } else {
                            Log.d("KeypadScreen", "Dial Requested, asking for permission")
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(CallGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = SurfaceWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}
