package com.rajdialer.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Domain-level single source of truth for all calls.
 * Never exposes Android classes like android.telecom.Call.
 */
object CallManager {
    private val _calls = MutableStateFlow<Map<String, CallUiState>>(emptyMap())
    val calls: StateFlow<Map<String, CallUiState>> = _calls.asStateFlow()
    
    private val _activeCall = MutableStateFlow<CallUiState?>(null)
    val activeCall: StateFlow<CallUiState?> = _activeCall.asStateFlow()

    private var globalMuteState = false
    private var globalSpeakerState = false

    fun updateCallState(
        callId: String,
        number: String,
        name: String?,
        state: CallState,
        direction: CallDirection,
        connectedTimestamp: Long,
        supportedCapabilities: Int
    ) {
        _calls.update { currentMap ->
            val updatedMap = currentMap.toMutableMap()
            
            val existing = updatedMap[callId]
            
            if (state == CallState.DISCONNECTED) {
                updatedMap[callId] = existing?.copy(state = state) ?: CallUiState(
                    callId = callId, number = number, name = name, state = state, 
                    direction = direction, connectedTimestamp = connectedTimestamp,
                    supportedCapabilities = supportedCapabilities,
                    isMuted = globalMuteState, isSpeakerOn = globalSpeakerState
                )
            } else {
                // If we get a new name (resolved from UI layer), respect it over 'Unknown'
                val finalName = if (existing?.name != null && existing.name != "Unknown" && name == null) {
                    existing.name
                } else {
                    name
                }
                
                updatedMap[callId] = CallUiState(
                    callId = callId,
                    number = number,
                    name = finalName,
                    state = state,
                    direction = direction,
                    connectedTimestamp = connectedTimestamp,
                    supportedCapabilities = supportedCapabilities,
                    isMuted = globalMuteState,
                    isSpeakerOn = globalSpeakerState
                )
            }
            
            updatedMap
        }
        updateActiveCall()
    }
    
    // Allow UI layer to asynchronously inject a resolved name
    fun updateContactName(callId: String, resolvedName: String) {
        _calls.update { currentMap ->
            val updatedMap = currentMap.toMutableMap()
            updatedMap[callId]?.let {
                updatedMap[callId] = it.copy(name = resolvedName)
            }
            updatedMap
        }
        updateActiveCall()
    }

    fun updateAudioState(isMuted: Boolean, isSpeakerOn: Boolean) {
        globalMuteState = isMuted
        globalSpeakerState = isSpeakerOn
        
        _calls.update { currentMap ->
            val updatedMap = currentMap.toMutableMap()
            updatedMap.keys.forEach { id ->
                updatedMap[id] = updatedMap[id]!!.copy(
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn
                )
            }
            updatedMap
        }
        updateActiveCall()
    }
    
    fun removeCall(callId: String) {
        _calls.update { currentMap ->
            val updatedMap = currentMap.toMutableMap()
            updatedMap.remove(callId)
            updatedMap
        }
        updateActiveCall()
    }

    private fun updateActiveCall() {
        val currentCalls = _calls.value.values
        _activeCall.value = currentCalls.firstOrNull { it.state == CallState.ACTIVE }
            ?: currentCalls.firstOrNull { it.state == CallState.DIALING }
            ?: currentCalls.firstOrNull { it.state == CallState.RINGING }
            ?: currentCalls.firstOrNull { it.state == CallState.HOLDING }
            ?: currentCalls.firstOrNull() 
    }
}
