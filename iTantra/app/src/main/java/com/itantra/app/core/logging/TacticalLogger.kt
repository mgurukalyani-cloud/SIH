package com.itantra.app.core.logging

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: Level,
    val tag: String,
    val message: String
) {
    enum class Level { INFO, WARN, ERROR, DEBUG }

    fun formatted(): String {
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
        return "[$timeStr] [${level.name}] $tag: $message"
    }
}

object TacticalLogger {
    private const val MAX_LOGS = 200
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        append(LogEntry.Level.INFO, tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        append(LogEntry.Level.WARN, tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        Log.e(tag, msg, throwable)
        append(LogEntry.Level.ERROR, tag, if (throwable != null) "$msg: ${throwable.localizedMessage}" else msg)
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        append(LogEntry.Level.DEBUG, tag, msg)
    }

    private fun append(level: LogEntry.Level, tag: String, msg: String) {
        val entry = LogEntry(level = level, tag = tag, message = msg)
        val current = _logs.value.toMutableList()
        if (current.size >= MAX_LOGS) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
