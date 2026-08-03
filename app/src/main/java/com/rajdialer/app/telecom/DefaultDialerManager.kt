package com.rajdialer.app.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

/**
 * Manages the Default Dialer role request using Android 10+ RoleManager.
 */
class DefaultDialerManager(private val context: Context) {
    private val roleManager = context.getSystemService(RoleManager::class.java)

    /**
     * Checks if the application currently holds the Default Dialer role.
     */
    fun isDefaultDialer(): Boolean {
        return roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
    }

    /**
     * Returns the Intent to request the Default Dialer role, if available.
     */
    fun getDefaultDialerIntent(): Intent? {
        return if (roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else null
    }
}
