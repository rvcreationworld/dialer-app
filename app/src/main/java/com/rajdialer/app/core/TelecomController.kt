package com.rajdialer.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast

object TelecomController {
    
    fun placeCall(context: Context, number: String) {
        if (number.isBlank()) {
            Log.w("TelecomController", "Call Failed: Number is empty")
            return
        }

        try {
            Log.d("TelecomController", "Call Started: Dispatching ACTION_CALL for $number")
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: SecurityException) {
            Log.e("TelecomController", "Call Failed: SecurityException - Permission Denied", e)
            Toast.makeText(context, "Call permission denied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("TelecomController", "Call Failed: Unknown Error", e)
            Toast.makeText(context, "Unable to place call", Toast.LENGTH_SHORT).show()
        }
    }
}
