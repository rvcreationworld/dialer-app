package com.rajdialer.app.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajdialer.app.model.CallHistory
import com.rajdialer.app.data.network.ApiClient
import com.rajdialer.app.data.preferences.AppPreferences
import com.rajdialer.app.data.repository.CallLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class TaskItem(
    val id: Int,
    val leadId: Int,
    val leadName: String,
    val leadContact: String,
    val callNumber: Int,
    val createdAt: String,
    val deadlineAt: String?,
    val pendingAction: String?,
    val approvalStatus: String?,
    val isMsgSent: Boolean,
    val isRecordingSent: Boolean,
    val callTime: Long? = null,
    val matchedCallCount: Int = 0,
    val matchedCallDurations: List<Long> = emptyList(),
    val matchedCallDuration: Long? = null,
    val matchedCallTimestamp: Long? = null
)

@OptIn(FlowPreview::class)
class MyTasksViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val apiClient = ApiClient(prefs)
    private val callLogRepo = CallLogRepository(application)

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    private val _searchQueryFlow = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val filteredTasks: StateFlow<List<TaskItem>> = _filteredTasks
    

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query: String ->
                    val list = _tasks.value
                    if (query.isBlank()) {
                        _filteredTasks.value = list
                    } else {
                        val q: CharSequence = query
                        _filteredTasks.value = list.filter {
                            it.leadName.contains(q, ignoreCase = true) || it.leadContact.contains(q)
                        }
                    }
                }
        }
        
        viewModelScope.launch {
            _tasks.collect { list ->
                val query: CharSequence = _searchQueryFlow.value
                if (query.isBlank()) {
                    _filteredTasks.value = list
                } else {
                    _filteredTasks.value = list.filter {
                        it.leadName.contains(query, ignoreCase = true) || it.leadContact.contains(query)
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _searchQueryFlow.value = query
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchTasks()
                delay(10000) // Poll every 10 seconds
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchTasks() {
        viewModelScope.launch {
            if (_tasks.value.isEmpty()) {
                _isLoading.value = true
            }
            try {                val request = Request.Builder()
                    .url("${apiClient.getBaseUrl()}/api/telecaller/tasks")
                    .get()
                    .build()

                val response = withContext(Dispatchers.IO) {
                    apiClient.okHttpClient.newCall(request).execute()
                }
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    val jsonArray = JSONArray(bodyStr)
                    val list = mutableListOf<TaskItem>()
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    
                    // Fetch logs once for all tasks
                    val logs = try { callLogRepo.getCallLogs() } catch (e: Exception) { emptyList() }
                    
                    val activeTaskIds = mutableSetOf<Int>()
                    for (i in 0 until jsonArray.length()) {
                        activeTaskIds.add(jsonArray.getJSONObject(i).getInt("id"))
                    }

                    // Clean up stale click times for completed/non-existent tasks
                    val prefs = getApplication<android.app.Application>().getSharedPreferences("call_clicks", android.content.Context.MODE_PRIVATE)
                    val allKeys = prefs.all.keys
                    val editor = prefs.edit()
                    var changed = false
                    for (key in allKeys) {
                        if (key.startsWith("click_")) {
                            val tid = key.removePrefix("click_").toIntOrNull()
                            if (tid == null || !activeTaskIds.contains(tid)) {
                                editor.remove(key)
                                changed = true
                            }
                        }
                    }
                    if (changed) editor.apply()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        
                        val leadContact = obj.optString("lead_contact", "")
                        val taskCreatedAtStr = obj.optString("created_at", "")
                        val taskCallTimeMs = if (obj.has("call_time") && !obj.isNull("call_time")) obj.getLong("call_time") else null
                        
                        val matchingCalls = getMatchingCallsForTask(logs, leadContact, obj.getInt("id"))
                        val callCount = matchingCalls.size
                        val durations = matchingCalls.map { it.durationSeconds }
                        val totalDuration = if (matchingCalls.isNotEmpty()) durations.fold(0L) { acc, dur -> acc + dur } else null
                        val latestTimestamp = matchingCalls.lastOrNull()?.timestamp

                        list.add(
                            TaskItem(
                                id = obj.getInt("id"),
                                leadId = obj.getInt("lead_id"),
                                leadName = obj.optString("lead_name", "Unknown"),
                                leadContact = leadContact,
                                callNumber = obj.optInt("call_number", 1),
                                createdAt = taskCreatedAtStr,
                                deadlineAt = if (obj.has("deadline_at") && !obj.isNull("deadline_at")) obj.getString("deadline_at") else null,
                                pendingAction = if (obj.has("pending_action") && !obj.isNull("pending_action")) obj.getString("pending_action") else null,
                                approvalStatus = if (obj.has("approval_status") && !obj.isNull("approval_status")) obj.getString("approval_status") else null,
                                isMsgSent = obj.optInt("is_msg_sent", 0) == 1,
                                isRecordingSent = obj.optInt("is_recording_sent", 0) == 1,
                                callTime = taskCallTimeMs,
                                matchedCallCount = callCount,
                                matchedCallDurations = durations,
                                matchedCallDuration = totalDuration,
                                matchedCallTimestamp = latestTimestamp
                            )
                        )
                    }
                    _tasks.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fail silently on polling network error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun matchesPhoneNumber(callLogNumber: String, leadContact: String): Boolean {
        val cleanLog = callLogNumber.replace("[^0-9]".toRegex(), "")
        val cleanLead = leadContact.replace("[^0-9]".toRegex(), "")
        if (cleanLog.isEmpty() || cleanLead.isEmpty()) return false
        if (cleanLog == cleanLead) return true
        if (cleanLog.endsWith(cleanLead) || cleanLead.endsWith(cleanLog)) return true
        val last10Log = if (cleanLog.length >= 10) cleanLog.takeLast(10) else cleanLog
        val last10Lead = if (cleanLead.length >= 10) cleanLead.takeLast(10) else cleanLead
        return last10Log == last10Lead
    }

    private fun getMatchingCallsForTask(logs: List<CallHistory>, leadContact: String, taskId: Int): List<CallHistory> {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("call_clicks", android.content.Context.MODE_PRIVATE)
        val clickTime = prefs.getLong("click_$taskId", 0L)
        
        // STRICT RULE: If "Call Now" has not been clicked for this task yet, DO NOT match any calls
        if (clickTime <= 0L) return emptyList()

        // Only search for calls made after the telecaller clicked "Call Now" on this task
        val searchSince = clickTime - 2000L

        return logs.filter { log ->
            matchesPhoneNumber(log.number, leadContact) && log.timestamp >= searchSince
        }.sortedBy { it.timestamp }
    }

    fun uploadRecording(context: android.content.Context, taskId: Int, uri: android.net.Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val token = prefs.jwtToken
                val baseUrl = prefs.baseUrl
                val (success, message) = com.rajdialer.app.data.network.CallRecordingUploader.uploadRecording(context, taskId, uri, token, baseUrl)
                if (success) {
                    val currentTasks = _tasks.value.toMutableList()
                    val index = currentTasks.indexOfFirst { it.id == taskId }
                    if (index != -1) {
                        currentTasks[index] = currentTasks[index].copy(isRecordingSent = true)
                        _tasks.value = currentTasks
                    }
                    onComplete(true, message)
                } else {
                    _errorMessage.value = message
                    onComplete(false, message)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error"
                onComplete(false, e.localizedMessage ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTaskStatus(taskId: Int, status: String, contactNumber: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val logs = callLogRepo.getCallLogs()

                val matchingCalls = getMatchingCallsForTask(logs, contactNumber, taskId)
                
                var durationSecs = 0L
                var callTypeStr = "Outgoing"
                var callTimestamp = 0L
                
                if (matchingCalls.isNotEmpty()) {
                    durationSecs = matchingCalls.fold(0L) { acc, call -> acc + call.durationSeconds }
                    callTypeStr = matchingCalls.last().type.name.lowercase().replaceFirstChar { it.uppercase() }
                    callTimestamp = matchingCalls.last().timestamp
                }

                val json = JSONObject()
                json.put("status", status)
                json.put("call_duration", durationSecs)
                json.put("call_type", callTypeStr)
                json.put("call_timestamp", callTimestamp)
                json.put("call_attempts", matchingCalls.size)

                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("${apiClient.getBaseUrl()}/api/telecaller/tasks/$taskId/status")
                    .post(body)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    apiClient.okHttpClient.newCall(request).execute()
                }

                if (!response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    var errMsg = "Failed to update status"
                    try {
                        val errObj = JSONObject(respBody)
                        if (errObj.has("message")) {
                            errMsg = errObj.getString("message")
                        }
                    } catch (e: Exception) {
                        // Ignore JSON parse error, use default
                    }
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = errMsg
                        _isLoading.value = false
                    }
                    return@launch
                }
                
                // Clear the click time so the next call requires a fresh button click
                val prefs = getApplication<android.app.Application>().getSharedPreferences("call_clicks", android.content.Context.MODE_PRIVATE)
                prefs.edit().remove("click_$taskId").apply()

                // Optimistically remove the task from the UI so it disappears instantly
                val currentList = _tasks.value.toMutableList()
                currentList.removeAll { it.id == taskId }
                _tasks.value = currentList
                
                fetchTasks()
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Network error: Failed to update status"
                _isLoading.value = false
            }
        }
    }

    fun markMessageSent(taskId: Int) {
        viewModelScope.launch {
            try {
                val request = Request.Builder()
                    .url("${apiClient.getBaseUrl()}/api/telecaller/tasks/$taskId/msg-sent")
                    .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .build()

                withContext(Dispatchers.IO) {
                    apiClient.okHttpClient.newCall(request).execute()
                }
                
                // Fetch tasks to refresh the list and update the isMsgSent flag locally
                fetchTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun prepareWhatsAppMessage(task: TaskItem, onMessageReady: (String) -> Unit) {
        viewModelScope.launch {
            val duration = task.matchedCallDuration ?: 0L
            val msg = if (duration > 0) {
                "Hi! Thank you for your time today. 😊\nAs discussed on the call, here is the ShareShaala website where you can explore our services, courses, and trading resources:\nhttps://shareshaala.com\nIf you have any questions or need any assistance, simply reply to this message or call us.\nTeam ShareShaala"
            } else {
                "Hi! We tried reaching you regarding ShareShaala but couldn't connect.\nPlease let us know a convenient time to call you back, or you can explore our services here:\nhttps://shareshaala.com\nThank you!\nTeam ShareShaala"
            }
            
            withContext(Dispatchers.Main) {
                onMessageReady(msg)
            }
        }
    }

    fun recordCallClickTime(taskId: Int) {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("call_clicks", android.content.Context.MODE_PRIVATE)
        val existing = prefs.getLong("click_$taskId", 0L)
        // Preserve first click time if already recorded for this task!
        if (existing == 0L) {
            prefs.edit().putLong("click_$taskId", System.currentTimeMillis()).apply()
        }
    }
}
