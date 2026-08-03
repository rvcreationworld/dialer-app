package com.rajdialer.app.ui.ongoingcall

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajdialer.app.core.CallDirection
import com.rajdialer.app.core.CallState
import com.rajdialer.app.core.CallUiState

@Composable
fun OngoingCallScreen(viewModel: InCallViewModel) {
    val activeCall by viewModel.activeCall.collectAsState()
    val duration by viewModel.callDurationSeconds.collectAsState()

    // Dynamic gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF1E212D),
        targetValue = Color(0xFF2B3245),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "c1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF11141A),
        targetValue = Color(0xFF1C222D),
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "c2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color1, color2)))
    ) {
        if (activeCall == null) return

        val call = activeCall!!

        if (call.state == CallState.RINGING && call.direction == CallDirection.INCOMING) {
            IncomingCallUI(call, viewModel)
        } else {
            ActiveCallUI(call, duration, viewModel)
        }
    }
}

@Composable
fun IncomingCallUI(call: CallUiState, viewModel: InCallViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Incoming Call",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Avatar Pulsating
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = call.avatarInitials,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = call.name ?: "Unknown",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = call.number,
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { viewModel.rejectCall(call.callId) },
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0xFFFF3B30), CircleShape)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Decline", color = Color.White, fontSize = 16.sp)
            }

            // Accept Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { viewModel.answerCall(call.callId) },
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0xFF34C759), CircleShape)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Accept", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ActiveCallUI(call: CallUiState, duration: Long, viewModel: InCallViewModel) {
    var showKeypad by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = call.avatarInitials,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contact Info
        Text(
            text = call.name ?: "Unknown",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = call.number,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // State / Timer
        val stateText = when (call.state) {
            CallState.DIALING -> "Dialing..."
            CallState.RINGING -> "Ringing..."
            CallState.ACTIVE -> formatDuration(duration)
            CallState.HOLDING -> "On Hold"
            CallState.DISCONNECTED -> "Call Ended"
            else -> "Connecting..."
        }
        
        Text(
            text = stateText,
            fontSize = 20.sp,
            fontWeight = if (call.state == CallState.ACTIVE) FontWeight.Medium else FontWeight.Normal,
            color = if (call.state == CallState.DISCONNECTED) Color(0xFFFF5252) else Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Utility Controls
        AnimatedVisibility(
            visible = !showKeypad,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GlassButton(
                        icon = Icons.Default.MicOff,
                        label = "Mute",
                        isActive = call.isMuted,
                        onClick = { viewModel.toggleMute(call.isMuted) }
                    )
                    GlassButton(
                        icon = Icons.Default.Dialpad,
                        label = "Keypad",
                        isActive = false,
                        onClick = { showKeypad = true }
                    )
                    GlassButton(
                        icon = Icons.Default.VolumeUp,
                        label = "Speaker",
                        isActive = call.isSpeakerOn,
                        onClick = { viewModel.toggleSpeaker(call.isSpeakerOn) }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GlassButton(
                        icon = Icons.Default.Pause,
                        label = "Hold",
                        isActive = call.state == CallState.HOLDING,
                        onClick = { viewModel.toggleHold(call.callId) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // End Call Button
        IconButton(
            onClick = { viewModel.endCall(call.callId) },
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFFFF3B30), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Keypad Bottom Sheet Overlay
    if (showKeypad) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showKeypad = false }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFF22252D))
                    .padding(24.dp)
                    .clickable(enabled = false) {}, 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.width(40.dp).height(4.dp).background(Color.Gray.copy(alpha = 0.5f), CircleShape)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                val keys = listOf(
                    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                    listOf("*" to "", "0" to "+", "#" to "")
                )
                
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { (digit, letters) ->
                            KeypadButton(digit, letters) {
                                viewModel.playDtmf(call.callId, digit.first())
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    viewModel.stopDtmf(call.callId)
                                }, 200)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun GlassButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.15f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF1E212D) else Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun KeypadButton(digit: String, letters: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = digit, fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Medium)
        if (letters.isNotEmpty()) {
            Text(text = letters, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
