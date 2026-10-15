package com.dnic.geoscan.data.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.dnic.geoscan.data.model.ScanPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports in-memory scan sessions to standard CSV files saved in the device's Documents/dnic-GeoScan folder.
 */
class SessionExporter(
    private val context: Context
) {

    /**
     * Writes discrete scan points to a CSV file in Documents.
     *
     * @param points List of recorded [ScanPoint] items.
     * @param customPrefix Optional custom prefix for file name.
     * @return [Result] containing the absolute display path or filename upon success.
     */
    suspend fun exportToCsv(
        points: List<ScanPoint>,
        customPrefix: String = "dnic_geoscan"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot export an empty scan session."))
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${customPrefix}_$timestamp.csv"
        val headerTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val csvContent = buildString {
            appendLine("# dnic GeoScan Geophysical Magnetometer Survey")
            appendLine("# Exported Date: $headerTimestamp")
            appendLine("# Total Points: ${points.size}")
            appendLine("# Coordinate Format: X (col), Y (row), Z (microtesla uT), Timestamp (epoch ms)")
            appendLine("X,Y,Z,Timestamp")
            for (p in points) {
                appendLine("${p.x},${p.y},${p.zValue},${p.timestamp}")
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ Scoped Storage via MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/dnic-GeoScan")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry in Documents"))

                resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }

                Result.success("Documents/dnic-GeoScan/$fileName")
            } else {
                // Legacy Android storage
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetDir = File(documentsDir, "dnic-GeoScan")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val file = File(targetDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(csvContent.toByteArray(Charsets.UTF_8))
                    fos.flush()
                }

                Result.success(file.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
