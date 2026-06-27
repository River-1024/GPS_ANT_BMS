package com.zjsf.gps_ant_bms

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG_PREFIX = "GpsAntBms"
    private const val PREFS_NAME = "AppLoggerPrefs"
    private const val PREF_CRASH_PENDING = "crash_pending"
    private const val LOG_FILE_NAME = "gps_ant_bms_debug.log"
    private const val MAX_LOG_BYTES = 512 * 1024
    private const val MAX_MEMORY_LINES = 240

    private val lock = Any()
    private val memoryLines = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        i("AppLogger", "logger initialized, file=${logFile(context).absolutePath}")
    }

    fun logFile(context: Context): File = File(context.filesDir, LOG_FILE_NAME)

    fun readLog(maxChars: Int = 24000): String {
        val context = appContext ?: return "日志尚未初始化"
        return synchronized(lock) {
            runCatching {
                val fileText = logFile(context).takeIf { it.exists() }?.readText().orEmpty()
                val memoryText = memoryLines.joinToString("\n")
                val text = if (fileText.isNotBlank()) fileText else memoryText
                if (text.isBlank()) {
                    "暂无日志"
                } else {
                    text.takeLast(maxChars)
                }
            }.getOrElse { "读取日志失败: ${it.message}" }
        }
    }

    fun clearLog() {
        val context = appContext ?: return
        synchronized(lock) {
            memoryLines.clear()
            runCatching { logFile(context).writeText("") }
        }
    }

    fun markCrashPending() {
        val context = appContext ?: return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_CRASH_PENDING, true)
            .apply()
    }

    fun consumeCrashPending(): Boolean {
        val context = appContext ?: return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(PREF_CRASH_PENDING, false)
        if (pending) {
            prefs.edit().putBoolean(PREF_CRASH_PENDING, false).apply()
        }
        return pending
    }

    fun d(tag: String, message: String) {
        Log.d("$TAG_PREFIX/$tag", message)
        write("D", tag, message, null)
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX/$tag", message)
        write("I", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG_PREFIX/$tag", message, throwable)
        write("W", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG_PREFIX/$tag", message, throwable)
        write("E", tag, message, throwable)
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        val context = appContext ?: return
        synchronized(lock) {
            val timestamp = timeFormat.format(Date())
            val stack = throwable?.stackTraceToString()?.let { "\n$it" }.orEmpty()
            val line = "$timestamp $level/$tag: $message$stack"
            memoryLines.addLast(line)
            while (memoryLines.size > MAX_MEMORY_LINES) {
                memoryLines.removeFirst()
            }
            val writeResult = runCatching {
                val file = logFile(context)
                if (file.length() > MAX_LOG_BYTES) {
                    file.writeText("")
                }
                file.appendText("$line\n")
            }
            writeResult.onFailure {
                Log.e("$TAG_PREFIX/AppLogger", "failed to write log file", it)
            }
        }
    }
}
