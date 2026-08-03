package com.rajdialer.app.ui.recents

import com.rajdialer.app.data.repository.RingTimeStore
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajdialer.app.core.TelecomController
import com.rajdialer.app.ui.components.CallHistoryCard
import com.rajdialer.app.ui.components.SearchBar
import com.rajdialer.app.ui.theme.BackgroundWhite
import com.rajdialer.app.ui.theme.TextPrimary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(viewModel: RecentsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { isGranted ->
        if (isGranted.values.all { it }) {
            viewModel.loadCallLogs(context)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadCallLogs(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        SearchBar(
            query = uiState.searchQuery,
            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.padding(horizontal = 24.dp), 
            placeholder = "Search recents..."
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.permissionDenied) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Call Log permission is required to view recents.\nPlease grant it in Settings.",
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.searchQuery.isBlank()) "No recent calls." else "No matches found.",
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(
                    bottom = 120.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            ) {
                val grouped = uiState.filteredLogs.groupBy { it.dateGroup }
                
                grouped.forEach { (dateGroup, calls) ->
                    stickyHeader(key = dateGroup) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFAFFFFFF))
                                .padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = dateGroup,
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = Color(0xFF5F6368)
                            )
                        }
                    }
                    
                    itemsIndexed(calls, key = { _, call -> call.id }) { index, call ->
                        CallHistoryCard(
                            call = call, 
                            onClick = {
                                TelecomController.placeCall(context, call.number)
                            }
                        )
                        if (index < calls.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = Color(0xFFF1F3F4),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
