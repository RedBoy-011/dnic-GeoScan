package com.denic.geoscan.ui.screens.remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denic.geoscan.data.analysis.BicubicInterpolator
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

class RemoteScanViewModel(
    application: Application
) : AndroidViewModel(application) {

    val bluetoothService = BluetoothService(application.applicationContext)
    private val sessionExporter = SessionExporter(application.applicationContext)
    private val bicubicInterpolator = BicubicInterpolator(subdivisionFactor = 2)

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

        // Collect parsed ScanPoints when scanning/recording is active
        viewModelScope.launch {
            bluetoothService.scanPoints.collect { point ->
                _latestPoint.value = point
                if (_isScanning.value) {
                    _scannedPoints.value = _scannedPoints.value + point
                }
            }
        }
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

    fun disconnectDevice() {
        if (_isScanning.value) {
            stopScan()
        }
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
        }
    }

    fun clearData() {
        _scannedPoints.value = emptyList()
        _latestPoint.value = null
        _rawPacketPreview.value = "Buffer cleared. Ready."
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothService.disconnect()
    }
}
