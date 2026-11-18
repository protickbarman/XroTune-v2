package com.xrotune.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel(val label: String) {
    INFO("INFO"),
    API("API"),
    PLAYBACK("PLAY"),
    WARN("WARN"),
    ERROR("ERR")
}

data class AppLogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val detail: String? = null
)

object AppLogger {
    private const val MAX_LOGS = 300
    private val sequence = AtomicLong(1L)
    private val lock = Any()
    private val _logs = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val logs: StateFlow<List<AppLogEntry>> = _logs.asStateFlow()

    private fun append(level: LogLevel, tag: String, message: String, detail: String? = null) {
        val entry = AppLogEntry(
            id = sequence.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            detail = detail
        )
        synchronized(lock) {
            val updated = (listOf(entry) + _logs.value).take(MAX_LOGS)
            _logs.value = updated
        }
    }

    fun info(tag: String, message: String, detail: String? = null) =
        append(LogLevel.INFO, tag, message, detail)

    fun api(tag: String, message: String, detail: String? = null) =
        append(LogLevel.API, tag, message, detail)

    fun playback(tag: String, message: String, detail: String? = null) =
        append(LogLevel.PLAYBACK, tag, message, detail)

    fun warn(tag: String, message: String, detail: String? = null) =
        append(LogLevel.WARN, tag, message, detail)

    fun error(tag: String, message: String, detail: String? = null) =
        append(LogLevel.ERROR, tag, message, detail)

    fun clear() {
        synchronized(lock) {
            _logs.value = emptyList()
        }
    }

    fun formatAllForExport(entries: List<AppLogEntry> = _logs.value): String {
        val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return entries.joinToString("\n") { entry ->
            val ts = fmt.format(Date(entry.timestamp))
            val extra = entry.detail?.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()
            "[$ts] [${entry.level.label}] [${entry.tag}] ${entry.message}$extra"
        }
    }
}
