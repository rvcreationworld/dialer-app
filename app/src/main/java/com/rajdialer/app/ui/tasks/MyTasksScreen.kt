package com.rajdialer.app.ui.tasks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajdialer.app.core.TelecomController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(viewModel: MyTasksViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    var expandedDropdownId by remember { mutableStateOf<Int?>(null) }
    
    // Status list
    val statuses = listOf("Ringing", "Call Back", "Info Given", "Wrong No", "Int Angel", "Coded-Angel", "Coded-Dhan", "Think&LMK", "Not Int", "RdyKYC", "Under US", "Under Dhan")

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.fetchTasks()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.fetchTasks() // Fetch immediately on resume (e.g. after a call)
                viewModel.startPolling()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                viewModel.stopPolling()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPolling()
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Update Failed") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    TextButton(onClick = { viewModel.fetchTasks() }) {
                        Text("Refresh", color = Color(0xFFD81B60), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (tasks.isEmpty() && isLoading && errorMessage == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (tasks.isEmpty()) {
                Text(text = "No pending tasks", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search tasks...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                    
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            index = filteredTasks.indexOf(task),
                            expandedDropdownId = expandedDropdownId,
                            onDropdownClick = { 
                                expandedDropdownId = if (expandedDropdownId == task.id) null else task.id 
                            },
                            onMessageClick = {
                                viewModel.prepareWhatsAppMessage(it) { msg ->
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:${it.leadContact}")
                                        putExtra("sms_body", msg)
                                    }
                                    context.startActivity(intent)
                                    viewModel.markMessageSent(it.id)
                                }
                            },
                            onSaveStatus = { status ->
                                viewModel.updateTaskStatus(task.id, status, task.leadContact) {
                                    android.widget.Toast.makeText(context, "Status Updated", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                }
            }
            
            if (isLoading && tasks.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        // Removed BottomSheet logic
    }
}

@Composable
fun TaskCard(
    task: TaskItem, 
    index: Int,
    expandedDropdownId: Int?,
    onDropdownClick: () -> Unit,
    onMessageClick: (TaskItem) -> Unit,
    onSaveStatus: (String) -> Unit,
    viewModel: MyTasksViewModel
) {
    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf("Select Status...") }
    val statuses = listOf("Ringing", "Call Back", "Info Given", "Wrong No", "Int Angel", "Coded-Angel", "Coded-Dhan", "Think&LMK", "Not Int", "RdyKYC", "Under US", "Under Dhan")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Number indicator
            Text(
                text = "#${index + 1}",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp, top = 4.dp)
            )

            // Lead Info Left Column
            Column(modifier = Modifier.weight(0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFFE8EAF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = task.leadName.take(1).uppercase(),
                            color = Color(0xFF3F51B5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = task.leadName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = task.leadContact, fontSize = 14.sp, color = Color.DarkGray)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Call Now Button
                Button(
                    onClick = { 
                        viewModel.recordCallClickTime(task.id)
                        TelecomController.placeCall(context, task.leadContact) 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Box Right Column
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .border(2.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CALL ${task.callNumber} ACTION",
                        color = Color(0xFFD81B60),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    if (task.approvalStatus == "PENDING") {
                        Box(modifier = Modifier.background(Color(0xFFFFE0B2), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text("Pending ⏳", color = Color(0xFFE65100), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (!task.isMsgSent) {
                        Box(modifier = Modifier.background(Color(0xFFFFF9C4), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text("Msg Pending ⏳", color = Color(0xFFF57F17), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                if (task.approvalStatus == "PENDING") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sent for Admin Approval:\n${task.pendingAction ?: "Status"}",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Detected Call Log
                    if (task.matchedCallDuration != null && task.matchedCallTimestamp != null) {
                        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        val timeStr = timeFormat.format(java.util.Date(task.matchedCallTimestamp))
                        val m = task.matchedCallDuration / 60
                        val s = task.matchedCallDuration % 60
                        val durStr = if (m > 0) "${m}m ${s}s" else "${s}s"
                        
                        val logText = if (task.matchedCallCount > 1) {
                            val breakdown = task.matchedCallDurations.joinToString(" + ") { "${it}s" }
                            "${task.matchedCallCount} Calls: $breakdown = $durStr ($timeStr)"
                        } else {
                            "Detected Log: $durStr ($timeStr)"
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Phone, contentDescription = "Log", modifier = Modifier.size(10.dp), tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = logText,
                                fontSize = 9.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = "No Log", modifier = Modifier.size(10.dp), tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "No Call Detected Yet",
                                fontSize = 9.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Upload Recording Button
                    val isEligibleForUpload = task.matchedCallDuration != null && task.matchedCallDuration > 0
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            viewModel.uploadRecording(context, task.id, uri) { success ->
                                val msg = if (success) "Recording uploaded" else "Failed to upload"
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    
                    Button(
                        onClick = { launcher.launch("audio/*") },
                        enabled = isEligibleForUpload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (task.isRecordingSent) Color(0xFFF1F8E9) else Color(0xFFF3E5F5),
                            contentColor = if (task.isRecordingSent) Color(0xFF33691E) else Color(0xFF6A1B9A),
                            disabledContainerColor = Color(0xFFEEEEEE),
                            disabledContentColor = Color(0xFFAAAAAA)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(28.dp).padding(bottom = 6.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (task.isRecordingSent) "Recording Uploaded ✔" else if (!isEligibleForUpload) "Upload Recording (Call First)" else "Upload Recording",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Dropdown
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .clickable { onDropdownClick() }
                            .padding(8.dp)
                    ) {
                        Text(text = selectedStatus, fontSize = 12.sp, color = if (selectedStatus == "Select Status...") Color.Gray else Color.Black)
                        DropdownMenu(
                            expanded = expandedDropdownId == task.id,
                            onDismissRequest = onDropdownClick
                        ) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status, fontSize = 12.sp) },
                                    onClick = {
                                        selectedStatus = status
                                        onDropdownClick()
                                    },
                                    modifier = Modifier.height(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save Status Button
                    Button(
                        onClick = { 
                            if (!task.isMsgSent) {
                                onMessageClick(task)
                            } else if (selectedStatus == "Select Status...") {
                                android.widget.Toast.makeText(context, "Please select a status first.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                if (task.matchedCallTimestamp != null) {
                                    onSaveStatus(selectedStatus) 
                                } else {
                                    android.widget.Toast.makeText(context, "Please call the lead first. No call log detected.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        val buttonText = if (!task.isMsgSent) {
                            "Send Msg First"
                        } else if (listOf("Coded-Angel", "Coded-Dhan", "Not Int", "Wrong No").contains(selectedStatus)) {
                            "Send to Admin for Approval"
                        } else {
                            "Save Status"
                        }
                        Text(buttonText, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}
