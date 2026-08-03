package com.rajdialer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rajdialer.app.model.Contact
import com.rajdialer.app.ui.theme.AvatarBackground
import com.rajdialer.app.ui.theme.AvatarText
import com.rajdialer.app.ui.theme.PrimaryBlue
import com.rajdialer.app.ui.theme.TextPrimary
import com.rajdialer.app.ui.theme.TextSecondary
// Import for image loading if needed later, but keeping it simple for now

@Composable
fun ContactCard(
    contact: Contact, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AvatarBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.avatarInitials,
                color = AvatarText,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.number,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        if (contact.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
