package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.model.BluetoothConnectionState
import com.example.model.BluetoothDeviceInfo
import com.example.model.CommandLog
import com.example.model.LogType
import com.example.protocol.RoverProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import kotlin.random.Random

/**
 * Service managing Bluetooth Classic SPP connections for the HC-05 module,
 * as well as an interactive hardware simulation mode.
 */
class BluetoothService(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    private val _logs = MutableStateFlow<List<CommandLog>>(emptyList())
    val logs: StateFlow<List<CommandLog>> = _logs.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null
    private var simTelemetryJob: Job? = null

    private val sppUuid: UUID = UUID.fromString(RoverProtocol.SPP_UUID_STRING)

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { dev ->
                        val name = dev.name ?: "Unknown Device"
                        val address = dev.address ?: ""
                        val item = BluetoothDeviceInfo(name = name, address = address, isPaired = false)
                        val current = _discoveredDevices.value
                        if (current.none { it.address == address }) {
                            _discoveredDevices.value = current + item
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    addLog(LogType.INFO, "Device scanning completed.")
                }
            }
        }
    }

    private var isReceiverRegistered = false

    init {
        refreshPairedDevices()
        // If device has no hardware bluetooth adapter, start in simulation mode
        if (bluetoothAdapter == null) {
            _isSimulationMode.value = true
            addLog(LogType.INFO, "No physical Bluetooth adapter detected. Simulation Mode enabled.")
        } else {
            addLog(LogType.INFO, "Bluetooth Classic SPP service initialized.")
        }
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (_isSimulationMode.value || bluetoothAdapter == null) {
            _pairedDevices.value = listOf(
                BluetoothDeviceInfo(name = "HC-05-ROVER-6WD", address = "98:D3:31:F8:42:1A", isPaired = true),
                BluetoothDeviceInfo(name = "HC-05-ARM-DEV", address = "98:D3:32:21:09:BD", isPaired = true),
                BluetoothDeviceInfo(name = "Arduino-BT-Classic", address = "00:14:03:05:5A:F1", isPaired = true)
            )
            return
        }

        try {
            val bonded = bluetoothAdapter.bondedDevices
            val list = bonded?.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    isPaired = true
                )
            } ?: emptyList()
            _pairedDevices.value = list
        } catch (e: SecurityException) {
            addLog(LogType.ERROR, "Bluetooth permission not granted to list paired devices: ${e.message}")
        } catch (e: Exception) {
            addLog(LogType.ERROR, "Failed to get paired devices: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (_isSimulationMode.value || bluetoothAdapter == null) {
            _isScanning.value = true
            addLog(LogType.INFO, "Simulating Bluetooth scan...")
            scope.launch {
                delay(1200)
                _discoveredDevices.value = listOf(
                    BluetoothDeviceInfo(name = "HC-05-ROVER-NEW", address = "98:D3:33:88:11:AA", isPaired = false),
                    BluetoothDeviceInfo(name = "HC-06-ROBOTIC", address = "20:19:04:12:44:E2", isPaired = false)
                )
                _isScanning.value = false
                addLog(LogType.INFO, "Discovered 2 simulated devices.")
            }
            return
        }

        try {
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(discoveryReceiver, filter)
                isReceiverRegistered = true
            }

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            _discoveredDevices.value = emptyList()
            val started = bluetoothAdapter.startDiscovery()
            _isScanning.value = started
            if (started) {
                addLog(LogType.INFO, "Scanning for nearby Bluetooth devices...")
            } else {
                addLog(LogType.ERROR, "Could not start Bluetooth discovery.")
            }
        } catch (e: SecurityException) {
            addLog(LogType.ERROR, "Permission missing for Bluetooth scan: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (_isSimulationMode.value) {
            _isScanning.value = false
            return
        }
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (_: SecurityException) {
        }
        _isScanning.value = false
    }

    fun setSimulationMode(enabled: Boolean) {
        if (_isSimulationMode.value == enabled) return
        disconnect()
        _isSimulationMode.value = enabled
        addLog(LogType.INFO, if (enabled) "Switched to Simulation Mode." else "Switched to Hardware Bluetooth Mode.")
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDeviceInfo) {
        disconnect()
        _connectionState.value = BluetoothConnectionState.Connecting(device.name, device.address)
        addLog(LogType.INFO, "Connecting to ${device.name} [${device.address}]...")

        if (_isSimulationMode.value || bluetoothAdapter == null) {
            // Launch simulation connection
            scope.launch {
                delay(1000)
                _connectionState.value = BluetoothConnectionState.Connected(device.name, device.address)
                addLog(LogType.INFO, "Connected to ${device.name} (Simulation)")
                startSimulatedTelemetry()
            }
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                bluetoothAdapter.cancelDiscovery()
                val remoteDevice = bluetoothAdapter.getRemoteDevice(device.address)
                val socket = remoteDevice.createRfcommSocketToServiceRecord(sppUuid)
                socket.connect()

                activeSocket = socket
                outputStream = socket.outputStream

                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.Connected(device.name, device.address)
                    addLog(LogType.INFO, "Connected to ${device.name} via SPP.")
                }

                // Start reader loop
                readJob = launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))
                    while (isActive) {
                        try {
                            val line = reader.readLine() ?: break
                            withContext(Dispatchers.Main) {
                                addLog(LogType.RX, line)
                                _incomingLines.tryEmit(line)
                            }
                        } catch (e: Exception) {
                            if (isActive) {
                                withContext(Dispatchers.Main) {
                                    addLog(LogType.ERROR, "Connection lost: ${e.localizedMessage}")
                                    disconnect()
                                }
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.Error(e.localizedMessage ?: "Connection failed")
                    addLog(LogType.ERROR, "Failed to connect to ${device.name}: ${e.message}")
                }
                disconnectSocket()
            }
        }
    }

    fun disconnect() {
        simTelemetryJob?.cancel()
        simTelemetryJob = null
        readJob?.cancel()
        readJob = null

        disconnectSocket()

        val previous = _connectionState.value
        if (previous is BluetoothConnectionState.Connected) {
            addLog(LogType.INFO, "Disconnected from ${previous.deviceName}.")
        }
        _connectionState.value = BluetoothConnectionState.Disconnected
    }

    private fun disconnectSocket() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        outputStream = null

        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
    }

    fun sendCommand(command: String) {
        val trimmed = if (command.endsWith("\n")) command else "$command\n"
        val displayStr = trimmed.trimEnd()

        if (_isSimulationMode.value) {
            addLog(LogType.TX, displayStr)
            // If simulation is connected, acknowledge or simulate response
            if (_connectionState.value is BluetoothConnectionState.Connected) {
                // Echo received
            }
            return
        }

        val out = outputStream
        if (out == null || _connectionState.value !is BluetoothConnectionState.Connected) {
            addLog(LogType.ERROR, "Cannot send command: Rover not connected.")
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                out.write(trimmed.toByteArray(Charsets.UTF_8))
                out.flush()
                withContext(Dispatchers.Main) {
                    addLog(LogType.TX, displayStr)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addLog(LogType.ERROR, "Send failed: ${e.message}")
                }
            }
        }
    }

    private fun startSimulatedTelemetry() {
        simTelemetryJob?.cancel()
        simTelemetryJob = scope.launch {
            var simTemp = 24.2f
            var simHum = 58.5f
            var simSoil = 45
            var simGas = 140

            while (isActive && _connectionState.value is BluetoothConnectionState.Connected) {
                delay(1500)
                // Add realistic minor sensor fluctuations
                simTemp = (simTemp + Random.nextFloat() * 0.4f - 0.2f).coerceIn(20f, 35f)
                simHum = (simHum + Random.nextFloat() * 0.8f - 0.4f).coerceIn(40f, 85f)
                simSoil = (simSoil + Random.nextInt(-1, 2)).coerceIn(10, 90)
                simGas = (simGas + Random.nextInt(-3, 4)).coerceIn(80, 400)

                val packet = "D,%.1f,%.1f,%d,%d\n".format(simTemp, simHum, simSoil, simGas)
                val displayStr = packet.trimEnd()
                addLog(LogType.RX, displayStr)
                _incomingLines.tryEmit(packet)
            }
        }
    }

    fun addLog(type: LogType, message: String) {
        val entry = CommandLog(type = type, message = message)
        val current = _logs.value
        // Keep last 150 entries to conserve memory
        val trimmed = if (current.size > 150) current.takeLast(149) else current
        _logs.value = trimmed + entry
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun destroy() {
        disconnect()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }
}
