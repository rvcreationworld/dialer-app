package com.rajdialer.app.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rajdialer.app.core.TelecomController
import com.rajdialer.app.model.DummyData
import com.rajdialer.app.ui.components.ContactCard
import com.rajdialer.app.ui.components.SectionHeader
import com.rajdialer.app.ui.theme.BackgroundWhite

@Composable
fun FavoritesScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Favorites")
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(DummyData.favorites, key = { it.id }) { contact ->
                ContactCard(
                    contact = contact, 
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = {
                        TelecomController.placeCall(context, contact.number)
                    }
                )
            }
        }
    }
}
