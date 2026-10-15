package com.denic.geoscan.ui.screens.local

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denic.geoscan.data.analysis.BicubicInterpolator
import com.denic.geoscan.data.analysis.DatasetNormalizer
import com.denic.geoscan.data.model.ScanDataset
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.data.storage.LocalFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalAnalysisViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val fileParser = LocalFileParser()
    private val normalizer = DatasetNormalizer()
    private val bicubicInterpolator = BicubicInterpolator(subdivisionFactor = 2)

    private val _dataset = MutableStateFlow<ScanDataset?>(null)
    val dataset: StateFlow<ScanDataset?> = _dataset.asStateFlow()

    private val _isSmoothingEnabled = MutableStateFlow(false)
    val isSmoothingEnabled: StateFlow<Boolean> = _isSmoothingEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Smooth vs raw points computed dynamically for ECharts
    val visualizerPoints: StateFlow<List<ScanPoint>> = combine(
        _dataset,
        _isSmoothingEnabled
    ) { data, smoothing ->
        val points = data?.points?.map { ScanPoint(it.x, it.y, it.normalizedZ) } ?: emptyList()
        if (smoothing && points.size >= 4) {
            withContext(Dispatchers.Default) {
                bicubicInterpolator.interpolate(points)
            }
        } else {
            points
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleSmoothing() {
        _isSmoothingEnabled.value = !_isSmoothingEnabled.value
    }

    /**
     * Ingests, parses, and normalizes a magnetometer file chosen via Storage Access Framework (SAF).
     */
    fun loadFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver

                // Extract filename and size metadata
                var fileName = "scan_data.csv"
                var fileSizeFormatted = "Unknown size"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                        if (sizeIndex != -1) {
                            val sizeBytes = cursor.getLong(sizeIndex)
                            fileSizeFormatted = formatFileSize(sizeBytes)
                        }
                    }
                }

                val normalizedData = withContext(Dispatchers.IO) {
                    val stream = contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Unable to open input stream for selected file")

                    stream.use { inputStream ->
                        val rawPoints = fileParser.parse(inputStream)
                        if (rawPoints.isEmpty()) {
                            throw IllegalArgumentException("The selected file does not contain valid magnetometer coordinates or data points.")
                        }
                        normalizer.normalize(
                            rawPoints = rawPoints,
                            fileName = fileName,
                            fileSizeFormatted = fileSizeFormatted
                        )
                    }
                }

                _dataset.value = normalizedData
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to parse magnetometer file"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDataset() {
        _dataset.value = null
        _errorMessage.value = null
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
            else -> "$bytes bytes"
        }
    }
}
