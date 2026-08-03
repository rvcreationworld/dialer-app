package com.rajdialer.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajdialer.app.model.CallHistory
import com.rajdialer.app.model.CallType
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch

@Composable
fun CallHistoryCard(call: CallHistory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            val isMissed = call.type == CallType.MISSED
            val typeIcon = when (call.type) {
                CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
                else -> Icons.AutoMirrored.Filled.CallMissed
            }
            val typeTint = if (isMissed) Color(0xFFE53935) else Color(0xFF1E88E5)
            
            Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                Icon(imageVector = typeIcon, contentDescription = null, tint = typeTint, modifier = Modifier.size(18.dp))
            }
            
            // Name/Number details
            Column(modifier = Modifier.weight(1f)) {
                val title = if (call.name.isNotBlank() && call.name != "Unknown") call.name else call.number
                val baseSubtitle = if (title == call.name) call.number else ""
                val subtitle = if (baseSubtitle.isNotEmpty()) "$baseSubtitle • ${call.duration}" else call.duration
                
                Text(
                    text = title,
                    fontSize = 17.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Time & Badges
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(60.dp)) {
                Text(
                    text = call.timeOnly,
                    fontSize = 13.sp,
                    color = if (isMissed) Color(0xFFE53935) else Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = "SIM", tint = Color(0xFFA57B7B), modifier = Modifier.size(12.dp))
                }
            }
        }
        
        // Expandable Section
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ActionBadgeButton(icon = Icons.Default.Call, tint = Color(0xFF34C759), badge = true, onClick = onClick)
                    ActionBadgeButton(icon = Icons.Default.Message, tint = Color(0xFF007AFF), badge = false, onClick = {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("sms:${call.number}")
                        context.startActivity(intent)
                    })
                    ActionBadgeButton(icon = Icons.Default.Videocam, tint = Color(0xFF34C759), badge = true, onClick = { })
                    ActionBadgeButton(icon = Icons.Default.Menu, tint = Color(0xFF4A4A4A), badge = false, onClick = { })
                }
            }
        }
    }
}

@Composable
fun ActionBadgeButton(icon: ImageVector, tint: Color, badge: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F2F7))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        if (badge) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFA57B7B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
            }
        }
    }
}
