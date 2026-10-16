package com.denic.geoscan.ui.screens.remote

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denic.geoscan.data.analysis.BicubicInterpolator
import com.denic.geoscan.data.audio.AudioFeedbackManager
import com.denic.geoscan.data.audio.TelemetryAudioMode
import com.denic.geoscan.data.bluetooth.BluetoothConnectionState
import com.denic.geoscan.data.bluetooth.BluetoothDeviceModel
import com.denic.geoscan.data.bluetooth.BluetoothService
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.data.storage.SessionExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing Bluetooth live telemetry, automatic grid line stepping (X -> Y auto-increment),
 * acoustic audio feedback synthesis, and session persistence.
 */
class RemoteScanViewModel(
    application: Application
) : AndroidViewModel(application) {

    val bluetoothService = BluetoothService(application.applicationContext)
    private val sessionExporter = SessionExporter(application.applicationContext)
    private val bicubicInterpolator = BicubicInterpolator(subdivisionFactor = 2)
    val audioFeedbackManager = AudioFeedbackManager(application.applicationContext)

    private val prefs = application.getSharedPreferences("dnic_geoscan_settings", Context.MODE_PRIVATE)

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothService.connectionState

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = _pairedDevices.asStateFlow()

    private val _isDevicePickerVisible = MutableStateFlow(false)
    val isDevicePickerVisible: StateFlow<Boolean> = _isDevicePickerVisible.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedPoints = MutableStateFlow<List<ScanPoint>>(emptyList())
    val scannedPoints: StateFlow<List<ScanPoint>> = _scannedPoints.asStateFlow()

    private val _latestPoint = MutableStateFlow<ScanPoint?>(null)
    val latestPoint: StateFlow<ScanPoint?> = _latestPoint.asStateFlow()

    private val _rawPacketPreview = MutableStateFlow("Awaiting packet stream...")
    val rawPacketPreview: StateFlow<String> = _rawPacketPreview.asStateFlow()

    // Mathematical Bicubic Smoothing state
    private val _isSmoothingEnabled = MutableStateFlow(false)
    val isSmoothingEnabled: StateFlow<Boolean> = _isSmoothingEnabled.asStateFlow()

    // CSV Session Export state
    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // Audio Feedback Settings (DYNAMIC_Z_PITCH, SIMPLE_BEEP, MUTE)
    private val _audioMode = MutableStateFlow(
        TelemetryAudioMode.fromCode(
            prefs.getString("telemetry_audio_mode", TelemetryAudioMode.DYNAMIC_Z_PITCH.code)
                ?: TelemetryAudioMode.DYNAMIC_Z_PITCH.code
        )
    )
    val audioMode: StateFlow<TelemetryAudioMode> = _audioMode.asStateFlow()

    // Auto-Grid Sequencer: Points per Line (X steps)
    private val _targetPointsX = MutableStateFlow(
        prefs.getInt("grid_target_x", 10).coerceIn(2, 200)
    )
    val targetPointsX: StateFlow<Int> = _targetPointsX.asStateFlow()

    // Zigzag vs Parallel line acquisition mode
    private val _isZigzagMode = MutableStateFlow(
        prefs.getBoolean("grid_is_zigzag", false)
    )
    val isZigzagMode: StateFlow<Boolean> = _isZigzagMode.asStateFlow()

    // Current line index (Y): 0, 1, 2...
    val currentLineY: StateFlow<Int> = combine(_scannedPoints, _targetPointsX) { pts, xCount ->
        if (xCount > 0) pts.size / xCount else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Current step in line (X): 0 to xCount - 1
    val currentPointInLineX: StateFlow<Int> = combine(_scannedPoints, _targetPointsX) { pts, xCount ->
        if (xCount > 0) pts.size % xCount else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Points dispatched to ECharts (either raw or bicubic-interpolated)
    val visualizerPoints: StateFlow<List<ScanPoint>> = combine(
        _scannedPoints,
        _isSmoothingEnabled
    ) { points, smoothing ->
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

    init {
        // Collect raw packet strings for live monitor
        viewModelScope.launch {
            bluetoothService.rawPackets.collect { rawChunk ->
                _rawPacketPreview.value = rawChunk.trim()
            }
        }

        // Collect parsed ScanPoints and map to automatic grid coordinates (X -> Y auto-increment)
        viewModelScope.launch {
            bluetoothService.scanPoints.collect { rawPoint ->
                val currentCount = _scannedPoints.value.size
                val xTarget = _targetPointsX.value
                val autoY = currentCount / xTarget
                val rawXInLine = currentCount % xTarget
                val autoX = if (_isZigzagMode.value && autoY % 2 == 1) {
                    (xTarget - 1 - rawXInLine)
                } else {
                    rawXInLine
                }

                val gridPoint = rawPoint.copy(x = autoX, y = autoY)
                _latestPoint.value = gridPoint

                if (_isScanning.value) {
                    _scannedPoints.value = _scannedPoints.value + gridPoint
                }

                // Play acoustic telemetry feedback (Z-pitch, Simple beep, or Mute)
                audioFeedbackManager.playPointFeedback(rawPoint.zValue, _audioMode.value)
            }
        }

        // Announce Bluetooth connection voice notice upon establishing connection
        viewModelScope.launch {
            var wasConnected = false
            bluetoothService.connectionState.collect { state ->
                if (state is BluetoothConnectionState.Connected) {
                    if (!wasConnected) {
                        wasConnected = true
                        val appLang = prefs.getString("app_language", "en") ?: "en"
                        val isPersian = appLang.equals("fa", ignoreCase = true)
                        audioFeedbackManager.announceConnection(isPersian)
                    }
                } else {
                    wasConnected = false
                }
            }
        }
    }

    fun setAudioMode(mode: TelemetryAudioMode) {
        _audioMode.value = mode
        prefs.edit().putString("telemetry_audio_mode", mode.code).apply()
    }

    fun setTargetPointsX(x: Int) {
        val clamped = x.coerceIn(2, 200)
        _targetPointsX.value = clamped
        prefs.edit().putInt("grid_target_x", clamped).apply()
    }

    fun toggleZigzagMode() {
        val next = !_isZigzagMode.value
        _isZigzagMode.value = next
        prefs.edit().putBoolean("grid_is_zigzag", next).apply()
    }

    fun openDevicePicker() {
        refreshPairedDevices()
        _isDevicePickerVisible.value = true
    }

    fun closeDevicePicker() {
        _isDevicePickerVisible.value = false
    }

    fun refreshPairedDevices() {
        _pairedDevices.value = bluetoothService.getPairedDevices()
    }

    fun connectDevice(address: String) {
        _isDevicePickerVisible.value = false
        viewModelScope.launch {
            bluetoothService.connect(address, viewModelScope)
        }
    }

    private val _lastAutoSavePath = MutableStateFlow<String?>(null)
    val lastAutoSavePath: StateFlow<String?> = _lastAutoSavePath.asStateFlow()

    fun autoSaveCurrentSession() {
        val points = _scannedPoints.value
        if (points.isEmpty()) return

        viewModelScope.launch {
            val result = sessionExporter.autoSaveSession(points)
            result.onSuccess { file ->
                _lastAutoSavePath.value = file.absolutePath
                _exportStatusMessage.value = "ذخیره خودکار انجام شد: ${file.name}"
            }
        }
    }

    fun shareCurrentSession(context: Context) {
        val points = _scannedPoints.value
        if (points.isEmpty()) {
            _exportStatusMessage.value = "داده‌ای برای اشتراک‌گذاری وجود ندارد."
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            val result = sessionExporter.createShareIntent(points)
            result.fold(
                onSuccess = { intent ->
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        _exportStatusMessage.value = "خطا در اشتراک‌گذاری: ${e.localizedMessage}"
                    }
                },
                onFailure = { err ->
                    _exportStatusMessage.value = "خطا در آماده‌سازی فایل: ${err.localizedMessage}"
                }
            )
            _isExporting.value = false
        }
    }

    fun disconnectDevice() {
        if (_isScanning.value) {
            stopScan()
        }
        autoSaveCurrentSession()
        bluetoothService.disconnect()
    }

    fun toggleScan() {
        if (_isScanning.value) {
            stopScan()
        } else {
            startScan()
        }
    }

    fun toggleSmoothing() {
        _isSmoothingEnabled.value = !_isSmoothingEnabled.value
    }

    fun exportSession() {
        val points = _scannedPoints.value
        if (points.isEmpty()) {
            _exportStatusMessage.value = "Cannot export: scan session is empty."
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            val result = sessionExporter.exportToCsv(points)
            result.fold(
                onSuccess = { path ->
                    _exportStatusMessage.value = "Session exported to: $path"
                },
                onFailure = { error ->
                    _exportStatusMessage.value = "Export failed: ${error.localizedMessage}"
                }
            )
            _isExporting.value = false
        }
    }

    fun clearExportMessage() {
        _exportStatusMessage.value = null
    }

    private fun startScan() {
        _isScanning.value = true
        viewModelScope.launch {
            bluetoothService.sendCommand("START\n")
        }
    }

    private fun stopScan() {
        _isScanning.value = false
        viewModelScope.launch {
            bluetoothService.sendCommand("STOP\n")
            // Automatically persist collected session immediately upon stopping
            sessionExporter.autoSaveSession(_scannedPoints.value).onSuccess { file ->
                _lastAutoSavePath.value = file.absolutePath
            }
        }
    }

    fun clearData() {
        // Auto-save before clearing if points existed
        if (_scannedPoints.value.isNotEmpty()) {
            autoSaveCurrentSession()
        }
        _scannedPoints.value = emptyList()
        _latestPoint.value = null
        _rawPacketPreview.value = "Buffer cleared. Ready."
    }

    override fun onCleared() {
        super.onCleared()
        audioFeedbackManager.release()
        if (_scannedPoints.value.isNotEmpty()) {
            try {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                val targetDir = getApplication<Application>().getExternalFilesDir("Scans")
                    ?: java.io.File(getApplication<Application>().filesDir, "Scans")
                if (!targetDir.exists()) targetDir.mkdirs()
                val file = java.io.File(targetDir, "auto_saved_geoscan_$timestamp.csv")
                file.writeText(sessionExporter.generateCsvContent(_scannedPoints.value))
            } catch (_: Throwable) {
                // Ignore
            }
        }
        bluetoothService.disconnect()
    }
}
