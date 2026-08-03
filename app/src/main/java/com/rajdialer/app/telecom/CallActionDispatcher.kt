package com.rajdialer.app.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log

/**
 * Responsible for safely routing UI commands to the actual android.telecom.Call objects.
 * Keeps Android dependencies strictly inside the telecom layer.
 */
object CallActionDispatcher {
    private const val TAG = "CallActionDispatcher"
    
    private val activeCalls = mutableMapOf<String, Call>()
    private var inCallService: InCallService? = null

    fun setService(service: InCallService?) {
        inCallService = service
    }

    fun registerCall(callId: String, call: Call) {
        activeCalls[callId] = call
    }

    fun unregisterCall(callId: String) {
        activeCalls.remove(callId)
    }

    fun answerCall(callId: String) {
        val call = activeCalls[callId]
        if (call?.state == Call.STATE_RINGING) {
            call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    fun rejectCall(callId: String) {
        val call = activeCalls[callId]
        if (call?.state == Call.STATE_RINGING) {
            call.reject(false, null)
        }
    }

    fun disconnectCall(callId: String) {
        val call = activeCalls[callId]
        if (call != null) {
            if (call.state != Call.STATE_DISCONNECTED && call.state != Call.STATE_DISCONNECTING) {
                call.disconnect()
            }
        }
    }

    fun toggleMute(currentlyMuted: Boolean) {
        inCallService?.setMuted(!currentlyMuted)
    }

    fun toggleSpeaker(currentlySpeaker: Boolean) {
        val route = if (currentlySpeaker) {
            CallAudioState.ROUTE_EARPIECE
        } else {
            CallAudioState.ROUTE_SPEAKER
        }
        inCallService?.setAudioRoute(route)
    }

    fun toggleHold(callId: String) {
        val call = activeCalls[callId]
        if (call != null) {
            if (call.state == Call.STATE_ACTIVE) {
                call.hold()
            } else if (call.state == Call.STATE_HOLDING) {
                call.unhold()
            }
        }
    }

    fun playDtmfTone(callId: String, digit: Char) {
        activeCalls[callId]?.playDtmfTone(digit)
    }

    fun stopDtmfTone(callId: String) {
        activeCalls[callId]?.stopDtmfTone()
    }
}
