package com.rajdialer.app.ui.ongoingcall

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rajdialer.app.core.CallManager
import com.rajdialer.app.ui.theme.RajDialerTheme
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {

    companion object {
        var isActivityVisible = false
            private set
    }
    
    private val viewModel: InCallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("InCallActivity", "onCreate: Launched dedicated InCall UI")
        
        // Let Android know we want to show up even over the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Do NOT call requestDismissKeyguard, as that forces the user to unlock!
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContent {
            RajDialerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OngoingCallScreen(viewModel = viewModel)
                }
            }
        }
        
        // Auto-finish activity if there are no active calls
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CallManager.calls.collect { calls ->
                    if (calls.isEmpty()) {
                        Log.d("InCallActivity", "No remaining calls. Finishing InCallActivity.")
                        finish()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isActivityVisible = true
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
    }
}
