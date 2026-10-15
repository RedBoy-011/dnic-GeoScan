package com.denic.geoscan.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.denic.geoscan.data.model.ScanPoint
import com.denic.geoscan.data.parser.ScanDataParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Service managing Bluetooth Serial Port Profile (SPP) connections,
 * background data streaming, and telemetry parsing using Kotlin Coroutines and SharedFlow.
 */
class BluetoothService(
    private val context: Context,
    private val dataParser: ScanDataParser = ScanDataParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        // Standard Serial Port Profile (SPP) UUID used by HC-05 and similar serial modules
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val BUFFER_SIZE = 1024
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    // SharedFlow for live real-time scan points streaming to ViewModels and UI visualizer
    private val _scanPoints = MutableSharedFlow<ScanPoint>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val scanPoints: SharedFlow<ScanPoint> = _scanPoints.asSharedFlow()

    // SharedFlow emitting raw packet chunks for live stream monitoring preview
    private val _rawPackets = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val rawPackets: SharedFlow<String> = _rawPackets.asSharedFlow()

    private var activeSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var streamReadingJob: Job? = null

    /**
     * Retrieves all paired Bluetooth devices available on the device.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceModel> {
        return try {
            val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
            bonded.map { device ->
                BluetoothDeviceModel(
                    name = device.name ?: "Unknown Device",
                    address = device.address
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Connects to a remote SPP Bluetooth device (e.g. HC-05) asynchronously.
     *
     * @param deviceAddress Hardware MAC address of the target Bluetooth device.
     * @param scope CoroutineScope in which the streaming worker runs.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(
        deviceAddress: String,
        scope: CoroutineScope
    ): Result<Unit> = withContext(ioDispatcher) {
        disconnect()

        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter not available on this device"))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is disabled"))
        }

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Bluetooth device address: $deviceAddress", e))
        }

        val deviceName = try {
            device.name ?: "HC-05 ($deviceAddress)"
        } catch (_: SecurityException) {
            deviceAddress
        }

        _connectionState.value = BluetoothConnectionState.Connecting(
            deviceName = deviceName,
            deviceAddress = deviceAddress
        )

        // Cancel discovery to optimize SPP bandwidth and connection stability
        try {
            adapter.cancelDiscovery()
        } catch (_: SecurityException) {
            // Ignored if permission was restricted
        }

        try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            activeSocket = socket
            inputStream = socket.inputStream
            outputStream = socket.outputStream
            dataParser.reset()

            _connectionState.value = BluetoothConnectionState.Connected(
                deviceName = deviceName,
                deviceAddress = deviceAddress
            )

            // Launch streaming worker on the IO scope
            startDataStreaming(scope)
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            val errorMsg = e.localizedMessage ?: "Failed to connect to $deviceName"
            _connectionState.value = BluetoothConnectionState.Error(errorMsg)
            Result.failure(e)
        }
    }

    /**
     * Reads incoming bytes continuously from the SPP socket and parses `$X,Y,Z#\n` packets.
     */
    private fun startDataStreaming(scope: CoroutineScope) {
        streamReadingJob?.cancel()
        streamReadingJob = scope.launch(ioDispatcher) {
            val buffer = ByteArray(BUFFER_SIZE)
            val stream = inputStream ?: return@launch

            try {
                while (isActive) {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead > 0) {
                        val chunk = String(buffer, 0, bytesRead, Charsets.US_ASCII)
                        _rawPackets.emit(chunk)

                        val parsedPoints = dataParser.parseChunk(chunk)
                        for (point in parsedPoints) {
                            _scanPoints.emit(point)
                        }
                    } else if (bytesRead == -1) {
                        // End of stream detected
                        break
                    }
                }
            } catch (e: IOException) {
                if (isActive) {
                    _connectionState.value = BluetoothConnectionState.Error("Stream read error: ${e.message}")
                }
            } finally {
                if (isActive) {
                    disconnect()
                }
            }
        }
    }

    /**
     * Sends command string to connected HC-05 (e.g. "START\n", "STOP\n", etc.)
     */
    suspend fun sendCommand(command: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            outputStream?.write(command.toByteArray(Charsets.US_ASCII))
            outputStream?.flush()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Closes socket and stream descriptors safely.
     */
    @Synchronized
    fun disconnect() {
        streamReadingJob?.cancel()
        streamReadingJob = null

        try {
            inputStream?.close()
        } catch (_: IOException) {}
        inputStream = null

        try {
            outputStream?.close()
        } catch (_: IOException) {}
        outputStream = null

        try {
            activeSocket?.close()
        } catch (_: IOException) {}
        activeSocket = null

        dataParser.reset()
        _connectionState.value = BluetoothConnectionState.Disconnected
    }
}
