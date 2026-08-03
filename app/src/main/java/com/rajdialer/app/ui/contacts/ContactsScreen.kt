package com.rajdialer.app.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajdialer.app.core.TelecomController
import com.rajdialer.app.ui.components.ContactCard
import com.rajdialer.app.ui.components.SearchBar
import com.rajdialer.app.ui.theme.BackgroundWhite
import com.rajdialer.app.ui.theme.PrimaryBlue
import com.rajdialer.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(viewModel: ContactsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.loadContacts(context) else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadContacts(context)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Fast Scroller State
    var alphabetHeight by remember { mutableIntStateOf(0) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toList()
    
    // Map initial letter to its LazyColumn index
    val letterIndices = remember(uiState.filteredContacts) {
        val indices = mutableMapOf<Char, Int>()
        var currentIndex = 0
        val grouped = uiState.filteredContacts.groupBy { it.name.firstOrNull()?.uppercase()?.firstOrNull() ?: '#' }
        grouped.forEach { (initial, contacts) ->
            // Normalize non-alphabets to '#'
            val key = if (initial in 'A'..'Z') initial else '#'
            if (!indices.containsKey(key)) {
                indices[key] = currentIndex
            }
            currentIndex += 1 + contacts.size // 1 for the header item, plus the contacts
        }
        indices
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.padding(horizontal = 24.dp), 
                placeholder = "Search contacts..."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Contacts", 
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp), 
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.filteredContacts.isNotEmpty()) {
                val grouped = uiState.filteredContacts.groupBy { 
                    val firstChar = it.name.firstOrNull()?.uppercase()?.firstOrNull() ?: '#'
                    if (firstChar in 'A'..'Z') firstChar else '#'
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 32.dp), 
                    contentPadding = PaddingValues(
                        bottom = 120.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    grouped.forEach { (initial, contacts) ->
                        item {
                            Text(
                                text = initial.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9F9))
                                    .padding(vertical = 4.dp)
                            )
                        }
                        items(contacts, key = { it.id }) { contact ->
                            ContactCard(
                                contact = contact,
                                onClick = { TelecomController.placeCall(context, contact.number) },
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else if (uiState.searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matches found.", color = TextPrimary)
                }
            }
        }

        // Fast Scroller Alphabet Strip
        if (uiState.filteredContacts.isNotEmpty() && uiState.searchQuery.isBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .onGloballyPositioned { alphabetHeight = it.size.height }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var y = down.position.y
                            if (alphabetHeight > 0) {
                                val charIndex = (y / (alphabetHeight / alphabet.size.toFloat())).toInt().coerceIn(0, alphabet.size - 1)
                                val char = alphabet[charIndex]
                                selectedLetter = char
                                letterIndices[char]?.let { idx ->
                                    coroutineScope.launch { listState.scrollToItem(idx) }
                                }
                            }
                            
                            do {
                                val event = awaitPointerEvent()
                                y = event.changes.first().position.y
                                if (alphabetHeight > 0) {
                                    val newIndex = (y / (alphabetHeight / alphabet.size.toFloat())).toInt().coerceIn(0, alphabet.size - 1)
                                    val newChar = alphabet[newIndex]
                                    if (newChar != selectedLetter) {
                                        selectedLetter = newChar
                                        letterIndices[newChar]?.let { idx ->
                                            coroutineScope.launch { listState.scrollToItem(idx) }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            
                            selectedLetter = null
                        }
                    },
                verticalArrangement = Arrangement.Center
            ) {
                alphabet.forEach { letter ->
                    Text(
                        text = letter.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp)
                    )
                }
            }
            
            // Selected Letter Bubble Indicator
            selectedLetter?.let { letter ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp)
                        .background(Color(0xB3000000), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
