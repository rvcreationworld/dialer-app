package com.rajdialer.app.data.network

import com.rajdialer.app.data.preferences.AppPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

class ApiClient(private val prefs: AppPreferences) {

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // If the URL requires API token (e.g., contains /api/)
        if (originalRequest.url.encodedPath.contains("/api/")) {
            val token = prefs.jwtToken
            if (token.isNotEmpty()) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                return@Interceptor chain.proceed(newRequest)
            }
        }
        chain.proceed(originalRequest)
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getBaseUrl(): String {
        var url = prefs.baseUrl
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        if (url.endsWith("/")) {
            url = url.dropLast(1)
        }
        return url
    }
}
