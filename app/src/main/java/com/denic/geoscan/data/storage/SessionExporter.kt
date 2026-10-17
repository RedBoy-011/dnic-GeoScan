package com.denic.geoscan.data.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.denic.geoscan.data.model.ScanPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports in-memory scan sessions to standard CSV files saved in the device's Documents/denic-GeoScan folder,
 * provides rock-solid auto-saving to local app storage, and prepares sharing intents.
 */
class SessionExporter(
    private val context: Context
) {

    fun generateCsvContent(points: List<ScanPoint>): String {
        val headerTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            appendLine("# denic GeoScan Geophysical Magnetometer Survey")
            appendLine("# Exported Date: $headerTimestamp")
            appendLine("# Total Points: ${points.size}")
            appendLine("# Coordinate Format: X (col), Y (row), Z (microtesla uT), Timestamp (epoch ms)")
            appendLine("X,Y,Z,Timestamp")
            for (p in points) {
                appendLine("${p.x},${p.y},${p.zValue},${p.timestamp}")
            }
        }
    }

    /**
     * Writes discrete scan points to a CSV file in Documents.
     *
     * @param points List of recorded [ScanPoint] items.
     * @param customPrefix Optional custom prefix for file name.
     * @return [Result] containing the absolute display path or filename upon success.
     */
    suspend fun exportToCsv(
        points: List<ScanPoint>,
        customPrefix: String = "denic_geoscan"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot export an empty scan session."))
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${customPrefix}_$timestamp.csv"
        val csvContent = generateCsvContent(points)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ Scoped Storage via MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/denic-GeoScan")
                }

                val resolver = context.contentResolver
                val contentUri = try {
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } catch (_: Exception) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                val uri = resolver.insert(contentUri, contentValues)
                    ?: resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create storage entry in Documents"))

                resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }

                Result.success("Documents/denic-GeoScan/$fileName")
            } else {
                // Legacy Android storage
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val targetDir = File(documentsDir, "denic-GeoScan")
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

    /**
     * Automatically saves the current session directly to dedicated app storage
     * without requiring any permissions, guaranteeing data persistence before app exit.
     */
    suspend fun autoSaveSession(
        points: List<ScanPoint>,
        customPrefix: String = "auto_saved_geoscan"
    ): Result<File> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot auto-save empty points."))
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${customPrefix}_$timestamp.csv"
            val targetDir = context.getExternalFilesDir("Scans") ?: File(context.filesDir, "Scans")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val file = File(targetDir, fileName)
            file.writeText(generateCsvContent(points))

            // Background attempt to also register in MediaStore Documents for user convenience
            try {
                exportToCsv(points, customPrefix)
            } catch (_: Throwable) {
                // Best-effort
            }

            Result.success(file)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Prepares an Android Chooser Intent using FileProvider to share the scan CSV
     * with Telegram, WhatsApp, Email, Drive, or external storage apps.
     */
    suspend fun createShareIntent(
        points: List<ScanPoint>,
        customPrefix: String = "geoscan_survey"
    ): Result<android.content.Intent> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No scan points to share."))
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${customPrefix}_$timestamp.csv"
            val cacheDir = File(context.cacheDir, "shared_scans").apply { if (!exists()) mkdirs() }
            val file = File(cacheDir, fileName)
            file.writeText(generateCsvContent(points))

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Dnic-GeoScan Data Export ($timestamp)")
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    "داده‌های خروجی اسکنر ژئوفیزیک Dnic-GeoScan (شامل ${points.size} نقطه پردازش‌شده)."
                )
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = android.content.Intent.createChooser(
                shareIntent,
                "اشتراک‌گذاری داده‌های اسکن Dnic-GeoScan"
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Result.success(chooser)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Exports bundled demo scans (CSV and V3D) directly to the device's public Downloads directory.
     */
    suspend fun saveAssetDemoFiles(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val assetManager = context.assets
            val demoFiles = listOf(
                "demo_10x10_anomaly.csv",
                "demo_10x10_anomaly.v3d",
                "demo_10x10_matrix.csv",
                "demo_mine_multidepth.csv",
                "demo_mine_multidepth.v3d"
            )
            var savedCount = 0

            for (fileName in demoFiles) {
                try {
                    val input = assetManager.open("demo_scans/$fileName")
                    val content = input.bufferedReader().use { it.readText() }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(
                                MediaStore.MediaColumns.MIME_TYPE,
                                if (fileName.endsWith(".v3d")) "application/octet-stream" else "text/csv"
                            )
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Dnic-GeoScan")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let { targetUri ->
                            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                                out.write(content.toByteArray(Charsets.UTF_8))
                                out.flush()
                            }
                            savedCount++
                        }
                    } else {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val targetDir = File(downloadsDir, "Dnic-GeoScan")
                        if (!targetDir.exists()) targetDir.mkdirs()
                        val file = File(targetDir, fileName)
                        file.writeText(content, Charsets.UTF_8)
                        savedCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SessionExporter", "Failed to copy demo asset: $fileName", e)
                }
            }

            if (savedCount > 0) {
                Result.success("Download/Dnic-GeoScan ($savedCount فایل دمو ذخیره شد)")
            } else {
                Result.failure(IllegalStateException("امکان ذخیره فایل‌های دمو وجود نداشت."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

