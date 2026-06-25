package com.example.scheduler

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.Serializable

object NotificationLogger {
    private const val LOG_FILE_NAME = "notification_delivery_logs.json"
    private const val MAX_LOGS = 100

    data class LogEntry(
        val type: String, // "scheduled", "triggered", "dismissed", "failed"
        val timestamp: Long,
        val details: String
    ) : Serializable

    @Synchronized
    fun log(context: Context, type: String, details: String) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val logs = readLogs(context).toMutableList()
            
            logs.add(0, LogEntry(type, System.currentTimeMillis(), details))
            
            // Limit to maximum size
            val trimmed = if (logs.size > MAX_LOGS) logs.take(MAX_LOGS) else logs
            
            val jsonArray = JSONArray()
            for (entry in trimmed) {
                val obj = JSONObject().apply {
                    put("type", entry.type)
                    put("timestamp", entry.timestamp)
                    put("details", entry.details)
                }
                jsonArray.put(obj)
            }
            
            file.writeText(jsonArray.toString())
            Log.d("NotificationLogger", "Logged notification event: [$type] $details")
        } catch (e: Exception) {
            Log.e("NotificationLogger", "Failed to write log: ${e.message}")
        }
    }

    @Synchronized
    fun readLogs(context: Context): List<LogEntry> {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<LogEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LogEntry(
                        type = obj.getString("type"),
                        timestamp = obj.getLong("timestamp"),
                        details = obj.getString("details")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("NotificationLogger", "Failed to read logs: ${e.message}")
            emptyList()
        }
    }

    @Synchronized
    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e("NotificationLogger", "Failed to clear logs: ${e.message}")
        }
    }
}
