package com.rajdialer.app.data.repository

import android.content.Context
import android.provider.CallLog
import com.rajdialer.app.model.CallHistory
import com.rajdialer.app.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CallLogRepository(private val context: Context) {

    suspend fun getCallLogs(): List<CallHistory> = withContext(Dispatchers.IO) {
        val callLogs = mutableListOf<CallHistory>()
        
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )
        

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            CallLog.Calls.DATE + " DESC" 
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(CallLog.Calls._ID)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            
            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val groupFormat = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
            
            var count = 0
            while (it.moveToNext() && count < 150) {
                val id = it.getString(idIndex) ?: continue
                count++
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex) ?: "Unknown"
                val dateMillis = it.getLong(dateIndex)
                val durationSeconds = it.getLong(durationIndex)
                val typeInt = it.getInt(typeIndex)
                
                val callType = when (typeInt) {
                    CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                    else -> CallType.INCOMING
                }
                
                val timeString = dateFormat.format(Date(dateMillis))
                val timeOnlyStr = timeFormat.format(Date(dateMillis))
                
                val calendar = Calendar.getInstance()
                val today = calendar.get(Calendar.DAY_OF_YEAR)
                val todayYear = calendar.get(Calendar.YEAR)
                
                calendar.timeInMillis = dateMillis
                val logDay = calendar.get(Calendar.DAY_OF_YEAR)
                val logYear = calendar.get(Calendar.YEAR)
                
                val dateGroupStr = when {
                    todayYear == logYear && today == logDay -> "Today"
                    todayYear == logYear && today - 1 == logDay -> "Yesterday"
                    else -> groupFormat.format(Date(dateMillis))
                }

                val durationString = if (durationSeconds > 0) {
                    val m = durationSeconds / 60
                    val s = durationSeconds % 60
                    if (m > 0) "${m}m ${s}s" else "${s}s"
                } else {
                    "0s"
                }

                callLogs.add(
                    CallHistory(
                        id = id,
                        name = if (name.isNullOrBlank()) number else name,
                        number = number,
                        time = timeString,
                        dateGroup = dateGroupStr,
                        timeOnly = timeOnlyStr,
                        timestamp = dateMillis,
                        duration = durationString,
                        durationSeconds = durationSeconds,
                        type = callType
                    )
                )
            }
        }
        
        return@withContext callLogs
    }
}
