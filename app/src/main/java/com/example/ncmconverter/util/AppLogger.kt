package com.example.ncmconverter.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ncmconverter.R
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object AppLogger {

    const val LEVEL_VERBOSE = 2
    const val LEVEL_DEBUG = 3
    const val LEVEL_INFO = 4
    const val LEVEL_WARN = 5
    const val LEVEL_ERROR = 6

    private const val MAX_LOG_ENTRIES = 1500
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()

    data class LogEntry(
        val timestamp: Long,
        val level: Int,
        val tag: String,
        val message: String,
        val tr: Throwable? = null
    ) {
        val levelString: String
            get() = when (level) {
                LEVEL_VERBOSE -> "V"
                LEVEL_DEBUG -> "D"
                LEVEL_INFO -> "I"
                LEVEL_WARN -> "W"
                LEVEL_ERROR -> "E"
                else -> "D"
            }
    }

    fun parseLevel(levelStr: String): Int = when (levelStr.uppercase()) {
        "VERBOSE" -> LEVEL_VERBOSE
        "DEBUG" -> LEVEL_DEBUG
        "INFO" -> LEVEL_INFO
        "WARN" -> LEVEL_WARN
        "ERROR" -> LEVEL_ERROR
        else -> LEVEL_DEBUG
    }

    fun v(tag: String, message: String, tr: Throwable? = null) = log(LEVEL_VERBOSE, tag, message, tr)
    fun d(tag: String, message: String, tr: Throwable? = null) = log(LEVEL_DEBUG, tag, message, tr)
    fun i(tag: String, message: String, tr: Throwable? = null) = log(LEVEL_INFO, tag, message, tr)
    fun w(tag: String, message: String, tr: Throwable? = null) = log(LEVEL_WARN, tag, message, tr)
    fun e(tag: String, message: String, tr: Throwable? = null) = log(LEVEL_ERROR, tag, message, tr)

    fun log(level: Int, tag: String, message: String, tr: Throwable? = null) {
        // Log to Android logcat
        when (level) {
            LEVEL_VERBOSE -> if (tr != null) Log.v(tag, message, tr) else Log.v(tag, message)
            LEVEL_DEBUG -> if (tr != null) Log.d(tag, message, tr) else Log.d(tag, message)
            LEVEL_INFO -> if (tr != null) Log.i(tag, message, tr) else Log.i(tag, message)
            LEVEL_WARN -> if (tr != null) Log.w(tag, message, tr) else Log.w(tag, message)
            LEVEL_ERROR -> if (tr != null) Log.e(tag, message, tr) else Log.e(tag, message)
        }

        // Store in memory ring buffer
        logBuffer.offer(LogEntry(System.currentTimeMillis(), level, tag, message, tr))
        while (logBuffer.size > MAX_LOG_ENTRIES) {
            logBuffer.poll()
        }
    }

    /**
     * 读取本应用进程的 logcat 输出与内部缓冲日志，并按指定级别过滤
     */
    fun getLogContent(context: Context, levelThreshold: String, format: String): String {
        val minLevel = parseLevel(levelThreshold)
        val logLines = mutableListOf<String>()

        // 1. 优先读取系统 logcat（仅过滤本进程 PID）
        val pid = Process.myPid().toString()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.contains(pid)) {
                    // 判断日志级别 (logcat 格式通常为: 09-19 10:01:14.517 D/tag(pid): ...)
                    val isPass = when {
                        minLevel <= LEVEL_VERBOSE -> true
                        minLevel <= LEVEL_DEBUG -> !l.contains(" V/")
                        minLevel <= LEVEL_INFO -> !l.contains(" V/") && !l.contains(" D/")
                        minLevel <= LEVEL_WARN -> l.contains(" W/") || l.contains(" E/") || l.contains(" F/")
                        minLevel <= LEVEL_ERROR -> l.contains(" E/") || l.contains(" F/")
                        else -> true
                    }
                    if (isPass) {
                        logLines.add(l)
                    }
                }
            }
            reader.close()
        } catch (_: Exception) {
            // logcat 读取受限时回退到内存缓冲
        }

        // 如果 logcat 为空或被系统拒绝，则使用应用内缓冲日志
        if (logLines.isEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            for (entry in logBuffer) {
                if (entry.level >= minLevel) {
                    val timeStr = dateFormat.format(Date(entry.timestamp))
                    val stackTrace = entry.tr?.let {
                        val sw = StringWriter()
                        it.printStackTrace(PrintWriter(sw))
                        "\n" + sw.toString()
                    } ?: ""
                    logLines.add("$timeStr ${entry.levelString}/${entry.tag}: ${entry.message}$stackTrace")
                }
            }
        }

        val logsText = if (logLines.isEmpty()) {
            context.getString(R.string.log_empty)
        } else {
            logLines.takeLast(1000).joinToString("\n")
        }

        // 2. 根据格式组织内容
        return formatLog(context, logsText, levelThreshold, format)
    }

    private fun formatLog(
        context: Context,
        rawLogs: String,
        levelThreshold: String,
        format: String
    ): String {
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: Exception) { null }

        val versionName = packageInfo?.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 0
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 0
        }

        return when (format.lowercase()) {
            "md" -> buildString {
                appendLine("# NCM Converter 运行日志")
                appendLine()
                appendLine("## 设备与环境")
                appendLine("| 属性 | 参数 |")
                appendLine("|---|---|")
                appendLine("| 应用版本 | $versionName ($versionCode) |")
                appendLine("| 系统版本 | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) |")
                appendLine("| 设备型号 | ${Build.MANUFACTURER} ${Build.MODEL} |")
                appendLine("| 架构 | ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"} |")
                appendLine("| 日志级别 | $levelThreshold |")
                appendLine("| 导出时间 | $nowStr |")
                appendLine()
                appendLine("## 日志堆栈")
                appendLine("```log")
                appendLine(rawLogs)
                appendLine("```")
            }
            "txt" -> buildString {
                appendLine("================================================================================")
                appendLine("NCM Converter 运行日志")
                appendLine("导出时间: $nowStr")
                appendLine("应用版本: $versionName ($versionCode)")
                appendLine("系统版本: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("设备型号: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("日志级别: $levelThreshold")
                appendLine("================================================================================")
                appendLine()
                appendLine(rawLogs)
            }
            else -> buildString { // .log
                appendLine("# NCM Converter Log - $nowStr - App: $versionName ($versionCode) - Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) - Level: $levelThreshold")
                appendLine(rawLogs)
            }
        }
    }

    /**
     * 导出日志文件并调起系统分享
     */
    fun exportAndShare(context: Context, levelThreshold: String, format: String) {
        try {
            val content = getLogContent(context, levelThreshold, format)
            val extension = when (format.lowercase()) {
                "md" -> "md"
                "txt" -> "txt"
                else -> "log"
            }
            val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "ncm_log_$timeTag.$extension"

            val logsDir = File(context.cacheDir, "logs").apply { if (!exists()) mkdirs() }
            val logFile = File(logsDir, fileName).apply {
                writeText(content)
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = when (extension) {
                    "md" -> "text/markdown"
                    else -> "text/plain"
                }
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.log_share_title)))
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to export logs", e)
            Toast.makeText(context, context.getString(R.string.log_export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 复制日志内容到剪贴板
     */
    fun copyToClipboard(context: Context, levelThreshold: String, format: String) {
        val content = getLogContent(context, levelThreshold, format)
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("NCM Converter Log", content))
        Toast.makeText(context, context.getString(R.string.log_copied), Toast.LENGTH_SHORT).show()
    }

    /**
     * 清理日志缓冲
     */
    fun clearLogs() {
        logBuffer.clear()
        try {
            Runtime.getRuntime().exec(arrayOf("logcat", "-c"))
        } catch (_: Exception) {}
    }
}
