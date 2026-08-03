package com.rajdialer.app.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("DialerPrefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("baseUrl", "https://telepro2.shareshaala.com") ?: "https://telepro2.shareshaala.com"
        set(value) = prefs.edit().putString("baseUrl", value).apply()

    var jwtToken: String
        get() = prefs.getString("jwtToken", "") ?: ""
        set(value) = prefs.edit().putString("jwtToken", value).apply()

    var telecallerId: Int
        get() = prefs.getInt("telecallerId", -1)
        set(value) = prefs.edit().putInt("telecallerId", value).apply()

    fun clearAuth() {
        prefs.edit().remove("jwtToken").remove("telecallerId").apply()
    }
}
