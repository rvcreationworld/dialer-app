package com.rajdialer.app.core

import android.content.Context
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

object SimManager {
    private const val PREFS_NAME = "sim_prefs"
    private const val KEY_SELECTED_SIM_ID = "selected_sim_id"

    fun getActiveSims(context: Context): List<PhoneAccountHandle> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        return telecomManager.callCapablePhoneAccounts
    }

    fun setSelectedSim(context: Context, handle: PhoneAccountHandle) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_SIM_ID, handle.id).apply()
    }

    fun hasSelectedSim(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_SELECTED_SIM_ID)
    }

    fun getSelectedSim(context: Context): PhoneAccountHandle? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_SELECTED_SIM_ID, null) ?: return null
        return getActiveSims(context).find { it.id == id }
    }

    fun getSimLabel(context: Context, handle: PhoneAccountHandle): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "SIM"
        }
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val phoneAccount = telecomManager.getPhoneAccount(handle)
        return phoneAccount?.label?.toString() ?: "SIM"
    }
}
