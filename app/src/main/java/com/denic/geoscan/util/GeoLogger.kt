package com.denic.geoscan.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * High-performance, persistent file-based logging subsystem for denic GeoScan.
 *
 * Capabilities:
 * 1. Synchronously & asynchronously appends structured log events to a physical text file (denic_debug_log.txt).
 * 2. Captures Kotlin logs, Android Logcat events, unhandled crashes, and JavaScript console errors from WebView.
 * 3. Maintains an in-memory ring-buffer of recent entries for live viewing in the App Settings.
 * 4. Exports debug logs to the public Downloads / Documents directory and prepares ACTION_SEND sharing intents.
 * 5. Automatically caps and rotates log file size (max 3 MB) to prevent storage bloat.
 */
object GeoLogger {

    private const val TAG = "GeoLogger"
    private const val LOG_FILE_NAME = "denic_debug_log.txt"
    private const val MAX_LOG_FILE_BYTES = 3 * 1024 * 1024 // 3 MB max before rotation
    private const val MAX_MEMORY_ENTRIES = 250

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var logFile: File? = null

    private val memoryLogQueue = ConcurrentLinkedQueue<String>()
    private val _recentLogs = MutableStateFlow<List<String>>(emptyList())
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    private val _logCount = MutableStateFlow(0)
    val logCount: StateFlow<Int> = _logCount.asStateFlow()

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app

        try {
            val baseDir = app.getExternalFilesDir(null) ?: app.filesDir
            val file = File(baseDir, LOG_FILE_NAME)
            logFile = file

            if (!file.exists()) {
                file.createNewFile()
                writeSystemHeader(file)
            } else if (file.length() > MAX_LOG_FILE_BYTES) {
                rotateLogFile(file)
            }

            // Load existing lines into memory queue (last 50 lines)
            try {
                file.useLines { lines ->
                    lines.takeLast(50).forEach { line ->
                        memoryLogQueue.offer(line)
                    }
                }
                _recentLogs.value = memoryLogQueue.toList()
                _logCount.value = memoryLogQueue.size
            } catch (_: Throwable) {}

            i(TAG, "=== GeoLogger initialized successfully (v2.0.0) ===")
            i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT}), Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize GeoLogger", e)
        }
    }

    private fun writeSystemHeader(file: File) {
        try {
            val header = buildString {
                appendLine("================================================================================")
                appendLine("denic GeoScan - Master Diagnostic & Telemetry Log")
                appendLine("Target App Version : 2.0.0 (Build 40)")
                appendLine("Initialized At     : ${dateFormat.format(Date())}")
                appendLine("Device Model       : ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
                appendLine("Hardware / Board   : ${Build.HARDWARE} / ${Build.BOARD}")
                appendLine("Android OS Version : ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})")
                appendLine("Build Fingerprint  : ${Build.FINGERPRINT}")
                appendLine("================================================================================")
                appendLine()
            }
            file.writeText(header)
        } catch (_: Throwable) {}
    }

    private fun rotateLogFile(file: File) {
        try {
            val backup = File(file.parentFile, "denic_debug_log.prev.txt")
            if (backup.exists()) backup.delete()
            file.renameTo(backup)
            file.createNewFile()
            writeSystemHeader(file)
        } catch (_: Throwable) {}
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        Log.d(tag, message, throwable)
        append("DEBUG", tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        Log.i(tag, message, throwable)
        append("INFO", tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        append("WARN", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        append("ERROR", tag, message, throwable)
    }

    fun recordException(tag: String, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        e(tag, "Exception caught: ${throwable.message}\n$sw")
    }

    /**
     * Dedicated logging for JavaScript console events inside ECharts / WebGL WebView.
     */
    fun logJsConsole(level: String, sourceId: String, lineNumber: Int, message: String) {
        val source = if (sourceId.isNotBlank()) sourceId.substringAfterLast("/") else "index.html"
        val formatted = "[JS:$source:$lineNumber] $message"
        when (level.uppercase()) {
            "ERROR" -> e("EChartsJS", formatted)
            "WARN" -> w("EChartsJS", formatted)
            else -> i("EChartsJS", formatted)
        }
    }

    /**
     * Dedicated logging for Bluetooth SPP connection lifecycle and packet acquisition.
     */
    fun logBluetooth(state: String, details: String) {
        i("BluetoothSPP", "State: $state • $details")
    }

    private fun append(level: String, tag: String, message: String, throwable: Throwable?) {
        val time = dateFormat.format(Date())
        val formattedLine = buildString {
            append("$time [$level] [$tag] $message")
            if (throwable != null) {
                append("\n")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                append(sw.toString())
            }
        }

        // 1. In-memory queue for live UI
        synchronized(memoryLogQueue) {
            memoryLogQueue.offer(formattedLine)
            while (memoryLogQueue.size > MAX_MEMORY_ENTRIES) {
                memoryLogQueue.poll()
            }
            _recentLogs.value = memoryLogQueue.toList()
            _logCount.value = memoryLogQueue.size
        }

        // 2. Physical disk persistence
        scope.launch {
            try {
                val file = logFile ?: return@launch
                if (file.length() > MAX_LOG_FILE_BYTES) {
                    rotateLogFile(file)
                }
                FileWriter(file, true).use { writer ->
                    writer.appendLine(formattedLine)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Exports the debug log file directly into the device's public Downloads directory
     * so that the user can find it in their My Files app.
     */
    fun exportLogToPublicDownloads(context: Context): Result<String> {
        return try {
            val file = logFile ?: return Result.failure(IllegalStateException("Log file not initialized"))
            if (!file.exists()) {
                return Result.failure(IllegalStateException("No log file found on disk"))
            }

            val content = file.readText()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "denic_debug_log.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Dnic-GeoScan")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(IllegalStateException("Could not create public file entry in Downloads"))

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                Result.success("Download/Dnic-GeoScan/denic_debug_log.txt")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "Dnic-GeoScan")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, "denic_debug_log.txt")
                targetFile.writeText(content, Charsets.UTF_8)
                Result.success(targetFile.absolutePath)
            }
        } catch (e: Throwable) {
            e(TAG, "Failed to export log to public storage", e)
            Result.failure(e)
        }
    }

    /**
     * Prepares an Android ACTION_SEND Intent with FileProvider Uri for instant sharing
     * via WhatsApp, Telegram, Gmail, Drive, Bluetooth, etc.
     */
    fun getShareIntent(context: Context): Intent? {
        return try {
            val file = logFile ?: return null
            if (!file.exists() || file.length() == 0L) {
                // Generate at least system header if empty
                file.createNewFile()
                writeSystemHeader(file)
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Dnic-GeoScan v2.0 Debug Diagnostic Log")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "denic GeoScan v2.0 Diagnostic Log attached.\nDevice: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Intent.createChooser(shareIntent, "Share Dnic-GeoScan Diagnostic Log")
        } catch (e: Throwable) {
            e(TAG, "Failed to create share intent for log file", e)
            null
        }
    }

    fun clearLog() {
        try {
            synchronized(memoryLogQueue) {
                memoryLogQueue.clear()
                _recentLogs.value = emptyList()
                _logCount.value = 0
            }
            val file = logFile
            if (file != null && file.exists()) {
                file.delete()
                file.createNewFile()
                writeSystemHeader(file)
            }
            i(TAG, "Debug logs cleared by user request")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }

    fun getLogFile(): File? = logFile

    fun getLogSizeFormatted(): String {
        val bytes = logFile?.length() ?: 0L
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
