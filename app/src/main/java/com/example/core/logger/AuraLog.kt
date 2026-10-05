package com.example.core.logger

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * AuraLog: Crash-safe, high-performance logger for Aura Music.
 * Retains a circular in-memory buffer of the latest 300 logs for diagnostic export
 * and ensures that no logging operation will ever crash the player loop or overlay window.
 */
object AuraLog {

    private const val MAX_LOG_HISTORY = 300
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var isInitialized = false

    fun initialize(context: Context) {
        isInitialized = true
        i("AuraLog", "AuraLog initialized successfully.")
    }

    fun v(tag: String, message: String) {
        log(Log.VERBOSE, tag, message, null)
    }

    fun d(tag: String, message: String) {
        log(Log.DEBUG, tag, message, null)
    }

    fun i(tag: String, message: String) {
        log(Log.INFO, tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.WARN, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.ERROR, tag, message, throwable)
    }

    private fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        try {
            val formattedMessage = if (throwable != null) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                "$message\n${sw}"
            } else {
                message
            }

            when (priority) {
                Log.VERBOSE -> Log.v(tag, formattedMessage)
                Log.DEBUG -> Log.d(tag, formattedMessage)
                Log.INFO -> Log.i(tag, formattedMessage)
                Log.WARN -> Log.w(tag, formattedMessage)
                Log.ERROR -> Log.e(tag, formattedMessage)
            }

            val timestamp = dateFormat.format(Date())
            val priorityChar = when (priority) {
                Log.VERBOSE -> 'V'
                Log.DEBUG -> 'D'
                Log.INFO -> 'I'
                Log.WARN -> 'W'
                Log.ERROR -> 'E'
                else -> 'U'
            }
            val entry = "[$timestamp] $priorityChar/$tag: $formattedMessage"

            logQueue.add(entry)
            while (logQueue.size > MAX_LOG_HISTORY) {
                logQueue.poll()
            }
        } catch (_: Throwable) {
            // Guarantee: logging failure never crashes execution
        }
    }

    /**
     * Returns snapshot of recent logs for debugging or export.
     */
    fun getRecentLogs(): List<String> {
        return logQueue.toList()
    }
}
