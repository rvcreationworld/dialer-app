package com.rajdialer.app.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RingTimeStore {
    private const val PREFS_NAME = "ring_time_prefs"
    
    // Flow to trigger UI refresh instantly when timer stops!
    private val _reloadTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val reloadTrigger = _reloadTrigger.asSharedFlow()
    
    private fun getLast10Digits(number: String): String {
        val digits = number.replace(Regex("[^0-9]"), "")
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }
    
    fun startTimer(context: Context, number: String) {
        val cleanNumber = getLast10Digits(number)
        if (cleanNumber.isNotEmpty()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong("${cleanNumber}_start", System.currentTimeMillis()).commit()
            Log.d("RingTimeStore", "Started timer for $cleanNumber")
        }
    }
    
    fun stopTimer(context: Context, number: String) {
        val cleanNumber = getLast10Digits(number)
        if (cleanNumber.isNotEmpty()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val startTime = prefs.getLong("${cleanNumber}_start", 0L)
            
            if (startTime > 0) {
                val totalSeconds = (System.currentTimeMillis() - startTime) / 1000
                prefs.edit()
                    .putLong(cleanNumber, totalSeconds)
                    .remove("${cleanNumber}_start")
                    .commit()
                Log.d("RingTimeStore", "Stopped timer for $cleanNumber. Total: $totalSeconds seconds")
                
                // Fire trigger to instantly reload UI and fix the race condition!
                _reloadTrigger.tryEmit(Unit)
            }
        }
    }
    
    fun getRingTime(context: Context, number: String, dateMillis: Long): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanNumber = getLast10Digits(number)
        if (cleanNumber.isEmpty() || !prefs.contains(cleanNumber)) return null
        
        return prefs.getLong(cleanNumber, 0L)
    }
}
