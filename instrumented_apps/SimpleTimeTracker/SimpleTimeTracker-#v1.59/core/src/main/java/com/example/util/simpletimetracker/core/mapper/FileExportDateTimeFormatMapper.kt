package com.example.util.simpletimetracker.core.mapper

import android.os.Build
import com.example.util.simpletimetracker.domain.fileExport.ExportDateTimeFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class FileExportDateTimeFormatMapper @Inject constructor() {

    private val formatLocal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val formatUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    private val formatTimeZone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    } else {
        formatLocal
    }

    fun mapDateTime(
        format: ExportDateTimeFormat,
        timestamp: Long,
    ): String {
        return mapToFormat(format).let {
            synchronized(it) {
                it.format(timestamp)
            }
        }
    }

    fun parseDateTime(
        format: ExportDateTimeFormat,
        timeString: String,
    ): Long? {
        return mapToFormat(format).let {
            synchronized(it) {
                runCatching { it.parse(timeString) }.getOrNull()?.time
            }
        }
    }

    fun isAvailable(format: ExportDateTimeFormat): Boolean {
        return when (format) {
            ExportDateTimeFormat.Local,
            ExportDateTimeFormat.Utc,
            -> true
            ExportDateTimeFormat.TimeZone,
            -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        }
    }

    fun getAvailableFormats(): List<ExportDateTimeFormat> {
        return listOf(
            ExportDateTimeFormat.Local,
            ExportDateTimeFormat.Utc,
            ExportDateTimeFormat.TimeZone,
        ).filter(::isAvailable)
    }

    private fun mapToFormat(
        format: ExportDateTimeFormat,
    ): SimpleDateFormat {
        return when (format) {
            ExportDateTimeFormat.Local -> formatLocal
            ExportDateTimeFormat.Utc -> formatUtc
            ExportDateTimeFormat.TimeZone -> formatTimeZone
        }
    }
}