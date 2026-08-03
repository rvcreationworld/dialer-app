package com.rajdialer.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdaterService {
    
    // Returns Pair<Int, String>? -> (VersionCode, DownloadUrl) or null if no update
    suspend fun checkForUpdates(currentVersionCode: Int): Pair<Int, String>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/rvcreationworld/dialer-app/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                // Parse tag_name, expecting format like "v2" or "2"
                val tagName = json.getString("tag_name")
                val latestVersionCode = tagName.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                
                if (latestVersionCode > currentVersionCode) {
                    val assets = json.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                        return@withContext Pair(latestVersionCode, downloadUrl)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
