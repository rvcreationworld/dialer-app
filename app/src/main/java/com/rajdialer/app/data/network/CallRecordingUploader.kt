package com.rajdialer.app.data.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object CallRecordingUploader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun uploadRecording(
        context: Context,
        taskId: Int,
        uri: Uri,
        token: String,
        baseUrl: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            // 1. Create a temporary file from the content URI
            val tempFile = getFileFromUri(context, uri) ?: return@withContext Pair(false, "Could not open file")

            // 2. Build the Multipart request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "recording",
                    tempFile.name,
                    tempFile.asRequestBody("audio/*".toMediaTypeOrNull())
                )
                .build()

            // 3. Build the Request
            val request = Request.Builder()
                .url("${baseUrl}/api/telecaller/tasks/$taskId/upload-recording")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            // 4. Execute
            val response = client.newCall(request).execute()
            
            // Clean up temp file
            if (tempFile.exists()) {
                tempFile.delete()
            }

            val body = response.body?.string()

            if (response.isSuccessful) {
                Log.d("CallRecordingUploader", "Upload successful")
                return@withContext Pair(true, "Recording uploaded successfully")
            } else {
                Log.e("CallRecordingUploader", "Upload failed: ${response.code} ${response.message}")
                Log.e("CallRecordingUploader", "Error body: $body")
                var errorMsg = "Failed to upload"
                try {
                    if (body != null) {
                        val json = org.json.JSONObject(body)
                        if (json.has("message")) {
                            errorMsg = json.getString("message")
                        }
                    }
                } catch (e: Exception) {
                    // Ignore JSON parsing errors
                }
                return@withContext Pair(false, errorMsg)
            }
        } catch (e: Exception) {
            Log.e("CallRecordingUploader", "Exception during upload", e)
            return@withContext Pair(false, e.localizedMessage ?: "Unknown error")
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(context, uri) ?: "recording.mp3"
            val tempFile = File(context.cacheDir, fileName)
            
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(tempFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("CallRecordingUploader", "Failed to create temp file", e)
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
