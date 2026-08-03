package com.rajdialer.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.rajdialer.app.core.CallDirection
import com.rajdialer.app.core.CallManager
import com.rajdialer.app.core.CallState
import com.rajdialer.app.ui.ongoingcall.InCallActivity
import com.rajdialer.app.data.repository.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RajInCallService : InCallService() {

    private val TAG = "RajInCallService"
    private val callCallbacks = mutableMapOf<Call, Call.Callback>()
    




    override fun onBind(intent: Intent?): android.os.IBinder? {
        Log.d(TAG, "onBind: Telecom bound to RajInCallService successfully.")
        CallActionDispatcher.setService(this)
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: Telecom unbound from RajInCallService.")
        CallActionDispatcher.setService(null)
        return super.onUnbind(intent)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            val isMuted = it.isMuted
            val isSpeaker = it.route == CallAudioState.ROUTE_SPEAKER
            CallManager.updateAudioState(isMuted, isSpeaker)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val callId = call.hashCode().toString()
        Log.d(TAG, "onCallAdded: ID=$callId, State=${call.state}")
        

        
        CallActionDispatcher.registerCall(callId, call)
        
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                super.onStateChanged(call, state)
                updateInternalManager(call, state)
            }
            
            override fun onDetailsChanged(call: Call, details: Call.Details) {
                super.onDetailsChanged(call, details)
                updateInternalManager(call, call.state)
            }
        }
        
        call.registerCallback(callback)
        callCallbacks[call] = callback
        
        updateInternalManager(call, call.state)

        if (call.state != Call.STATE_DISCONNECTED && call.state != Call.STATE_DISCONNECTING) {
            launchInCallActivity()
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        val callId = call.hashCode().toString()
        Log.d(TAG, "onCallRemoved: ID=$callId")
        

        
        callCallbacks.remove(call)?.let {
            call.unregisterCallback(it)
        }
        
        CallActionDispatcher.unregisterCall(callId)
        CallManager.removeCall(callId)
        
    }

    private fun updateInternalManager(call: Call, androidState: Int) {
        val internalState = mapToInternalState(androidState)
        val callId = call.hashCode().toString()
        
        val direction = if (call.details?.callDirection == Call.Details.DIRECTION_INCOMING) {
            CallDirection.INCOMING
        } else if (call.details?.callDirection == Call.Details.DIRECTION_OUTGOING) {
            CallDirection.OUTGOING
        } else {
            CallDirection.UNKNOWN
        }
        
        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val name = call.details?.callerDisplayName
        
        CallManager.updateCallState(
            callId = callId,
            number = number,
            name = name,
            state = internalState,
            direction = direction,
            connectedTimestamp = call.details?.connectTimeMillis ?: 0L,
            supportedCapabilities = call.details?.callCapabilities ?: 0
        )
        
        // Asynchronously look up the contact name if it's missing (especially for outgoing calls)
        if (name == null || name.isBlank() || name == "Unknown") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = ContactsRepository(this@RajInCallService)
                    val contacts = repository.getContacts()
                    val cleanNumber = number.replace(Regex("[^0-9+]"), "")
                    val matched = contacts.find { android.telephony.PhoneNumberUtils.compare(it.number, number) }
                    
                    if (matched != null) {
                        CallManager.updateCallState(
                            callId = callId,
                            number = number,
                            name = matched.name,
                            state = internalState,
                            direction = direction,
                            connectedTimestamp = call.details?.connectTimeMillis ?: 0L,
                            supportedCapabilities = call.details?.callCapabilities ?: 0
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Contact lookup failed: ${e.message}")
                }
            }
        }
        
    }

    private fun mapToInternalState(androidState: Int): CallState {
        return when (androidState) {
            Call.STATE_NEW -> CallState.IDLE
            Call.STATE_DIALING -> CallState.DIALING
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_HOLDING -> CallState.HOLDING
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallState.DISCONNECTED
            else -> CallState.IDLE
        }
    }

    private fun launchInCallActivity() {
        if (!InCallActivity.isActivityVisible) {
            val intent = Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
    }
}
