package com.rajdialer.app.ui.ongoingcall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajdialer.app.core.CallManager
import com.rajdialer.app.core.CallState
import com.rajdialer.app.core.CallUiState
import com.rajdialer.app.data.repository.ContactsRepository
import com.rajdialer.app.telecom.CallActionDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InCallViewModel(application: Application) : AndroidViewModel(application) {

    val activeCall: StateFlow<CallUiState?> = CallManager.activeCall
    
    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()
    
    private val contactsRepository = ContactsRepository(application)
    private val resolvedNumbers = mutableSetOf<String>()

    private var isTimerRunning = false
    private var localStartTime = 0L

    init {
        viewModelScope.launch {
            CallManager.activeCall.collect { call ->
                if (call == null) return@collect
                
                if ((call.name == null || call.name == "Unknown") && !resolvedNumbers.contains(call.number)) {
                    resolvedNumbers.add(call.number)
                    launch {
                        val contact = contactsRepository.getContacts().find { 
                            it.number.replace(Regex("[^0-9+]"), "") == call.number.replace(Regex("[^0-9+]"), "") 
                        }
                        if (contact != null) {
                            CallManager.updateContactName(call.callId, contact.name)
                        }
                    }
                }

                if (call.state == CallState.ACTIVE) {
                    val startTime = if (call.connectedTimestamp > 0) call.connectedTimestamp else {
                        if (localStartTime == 0L) localStartTime = System.currentTimeMillis()
                        localStartTime
                    }
                    startTimer(startTime)
                }
            }
        }
    }

    private fun startTimer(connectedTimestampMillis: Long) {
        if (isTimerRunning) return
        isTimerRunning = true
        
        viewModelScope.launch {
            while (isTimerRunning) {
                val currentActive = CallManager.activeCall.value
                if (currentActive == null || currentActive.state == CallState.DISCONNECTED) {
                    isTimerRunning = false
                    break
                }
                
                val durationMs = System.currentTimeMillis() - connectedTimestampMillis
                _callDurationSeconds.value = (durationMs / 1000).coerceAtLeast(0L)
                delay(1000)
            }
        }
    }

    fun answerCall(callId: String) = CallActionDispatcher.answerCall(callId)
    fun rejectCall(callId: String) = CallActionDispatcher.rejectCall(callId)
    fun endCall(callId: String) = CallActionDispatcher.disconnectCall(callId)
    
    fun toggleMute(currentlyMuted: Boolean) = CallActionDispatcher.toggleMute(currentlyMuted)
    
    fun toggleSpeaker(currentlySpeaker: Boolean) = CallActionDispatcher.toggleSpeaker(currentlySpeaker)
    
    fun toggleHold(callId: String) = CallActionDispatcher.toggleHold(callId)
    
    fun playDtmf(callId: String, digit: Char) = CallActionDispatcher.playDtmfTone(callId, digit)
    
    fun stopDtmf(callId: String) = CallActionDispatcher.stopDtmfTone(callId)
}
