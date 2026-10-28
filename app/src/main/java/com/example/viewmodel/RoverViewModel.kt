package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothService
import com.example.model.ArmJoint
import com.example.model.ArmPreset
import com.example.model.BluetoothConnectionState
import com.example.model.BluetoothDeviceInfo
import com.example.model.CommandLog
import com.example.model.DriveDirection
import com.example.model.LiquidGlassTheme
import com.example.model.LogType
import com.example.model.TelemetryData
import com.example.protocol.RoverProtocol
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class RoverUiState(
    val speed: Int = 180,
    val activeDirection: DriveDirection = DriveDirection.STOP,
    val armJoints: List<ArmJoint> = defaultArmJoints(),
    val latestTelemetry: TelemetryData = TelemetryData(),
    val telemetryHistory: List<TelemetryData> = emptyList(),
    val isAutoReconnect: Boolean = false,
    val emergencyStopTriggered: Boolean = false,
    val driveMode: DriveMode = DriveMode.HOLD_TO_RUN
)

enum class DriveMode {
    HOLD_TO_RUN, // Press and hold to drive, release to stop
    TOGGLE_RUN   // Tap to start driving, tap Stop to halt
}

private fun defaultArmJoints(): List<ArmJoint> = listOf(
    ArmJoint(index = 0, name = "Base (Waist)", role = "Horizontal Rotation (0°-180°)", angle = 90),
    ArmJoint(index = 1, name = "Shoulder", role = "Vertical Arm Lift (0°-180°)", angle = 90),
    ArmJoint(index = 2, name = "Elbow", role = "Forearm Extension (0°-180°)", angle = 90),
    ArmJoint(index = 3, name = "Wrist Pitch", role = "Up/Down Tilting (0°-180°)", angle = 90),
    ArmJoint(index = 4, name = "Wrist Roll", role = "Axial Rotation (0°-180°)", angle = 90),
    ArmJoint(index = 5, name = "Gripper Claw", role = "End Effector (0° Close - 180° Open)", angle = 120)
)

val DEFAULT_ARM_PRESETS = listOf(
    ArmPreset(name = "Home (90°)", description = "Centered neutral position for all servos", angles = listOf(90, 90, 90, 90, 90, 90), isCustom = false),
    ArmPreset(name = "Stowed", description = "Folded compact configuration for high-speed transit", angles = listOf(90, 30, 160, 20, 90, 20), isCustom = false),
    ArmPreset(name = "Ground Reach", description = "Lowered arm ready to scoop or collect samples", angles = listOf(90, 135, 140, 45, 90, 160), isCustom = false),
    ArmPreset(name = "High Reach", description = "Extended upward for tall inspection or sensors", angles = listOf(90, 60, 45, 120, 90, 100), isCustom = false),
    ArmPreset(name = "Open Claw", description = "Fully opened gripper jaws (180°)", angles = listOf(90, 90, 90, 90, 90, 180), isCustom = false),
    ArmPreset(name = "Close Claw", description = "Securely clamped gripper jaws (10°)", angles = listOf(90, 90, 90, 90, 90, 10), isCustom = false)
)

class RoverViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("rover_control_prefs", Context.MODE_PRIVATE)

    val bluetoothService = BluetoothService(application.applicationContext, viewModelScope)

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothService.connectionState
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothService.pairedDevices
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothService.discoveredDevices
    val isScanning: StateFlow<Boolean> = bluetoothService.isScanning
    val isSimulationMode: StateFlow<Boolean> = bluetoothService.isSimulationMode
    val commandLogs: StateFlow<List<CommandLog>> = bluetoothService.logs

    private val _uiState = MutableStateFlow(RoverUiState())
    val uiState: StateFlow<RoverUiState> = _uiState.asStateFlow()

    // Liquid Glass background theme selection
    private val _liquidTheme = MutableStateFlow(loadSavedTheme())
    val liquidTheme: StateFlow<LiquidGlassTheme> = _liquidTheme.asStateFlow()

    // Custom user arm presets
    private val _customPresets = MutableStateFlow<List<ArmPreset>>(loadSavedCustomPresets())
    val customPresets: StateFlow<List<ArmPreset>> = _customPresets.asStateFlow()

    // Throttling for arm sliders to avoid flooding serial buffer
    private val armSendJobs = mutableMapOf<Int, Job>()
    private var lastArmSendTime = 0L

    init {
        // Collect incoming lines from Bluetooth to parse telemetry
        viewModelScope.launch {
            bluetoothService.incomingLines.collect { line ->
                val parsed = RoverProtocol.parseTelemetry(line)
                if (parsed != null) {
                    _uiState.value = _uiState.value.let { state ->
                        val history = (state.telemetryHistory + parsed).takeLast(30)
                        state.copy(
                            latestTelemetry = parsed,
                            telemetryHistory = history
                        )
                    }
                }
            }
        }
    }

    fun setLiquidTheme(theme: LiquidGlassTheme) {
        _liquidTheme.value = theme
        prefs.edit().putString("liquid_glass_theme", theme.name).apply()
    }

    private fun loadSavedTheme(): LiquidGlassTheme {
        val saved = prefs.getString("liquid_glass_theme", null) ?: return LiquidGlassTheme.AURORA_CYAN
        return try {
            LiquidGlassTheme.valueOf(saved)
        } catch (_: Exception) {
            LiquidGlassTheme.AURORA_CYAN
        }
    }

    /**
     * Save the current angles of all 6 arm joints as a brand new custom user preset!
     */
    fun saveCurrentPoseAsPreset(name: String, description: String = "") {
        val currentAngles = _uiState.value.armJoints.map { it.angle }
        val newPreset = ArmPreset(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Preset #${_customPresets.value.size + 1}" },
            description = description.trim().ifEmpty { "Custom 6-axis configuration" },
            angles = currentAngles,
            isCustom = true
        )
        val updated = _customPresets.value + newPreset
        _customPresets.value = updated
        persistCustomPresets(updated)
        bluetoothService.addLog(LogType.INFO, "Saved custom arm preset '${newPreset.name}'")
    }

    /**
     * Remove a custom preset
     */
    fun deleteCustomPreset(presetId: String) {
        val updated = _customPresets.value.filterNot { it.id == presetId }
        _customPresets.value = updated
        persistCustomPresets(updated)
        bluetoothService.addLog(LogType.INFO, "Deleted arm preset")
    }

    private fun persistCustomPresets(presets: List<ArmPreset>) {
        try {
            val jsonArray = JSONArray()
            presets.forEach { preset ->
                val obj = JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("desc", preset.description)
                    val anglesArr = JSONArray()
                    preset.angles.forEach { anglesArr.put(it) }
                    put("angles", anglesArr)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("custom_presets_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadSavedCustomPresets(): List<ArmPreset> {
        val raw = prefs.getString("custom_presets_json", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<ArmPreset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val anglesArr = obj.getJSONArray("angles")
                val angles = mutableListOf<Int>()
                for (j in 0 until anglesArr.length()) {
                    angles.add(anglesArr.getInt(j))
                }
                list.add(
                    ArmPreset(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        description = obj.optString("desc", "Custom arm pose"),
                        angles = angles,
                        isCustom = true
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setSpeed(speed: Int) {
        val clamped = speed.coerceIn(0, 255)
        _uiState.value = _uiState.value.copy(speed = clamped)
        // If rover is actively driving, refresh motor command with new speed
        val currentDir = _uiState.value.activeDirection
        if (currentDir != DriveDirection.STOP) {
            val cmd = RoverProtocol.createMotorCommand(currentDir, clamped)
            bluetoothService.sendCommand(cmd)
        }
    }

    fun setDriveMode(mode: DriveMode) {
        _uiState.value = _uiState.value.copy(driveMode = mode)
    }

    /**
     * Start moving in given direction: "M,direction,speed\n"
     */
    fun startDriving(direction: DriveDirection) {
        val speed = _uiState.value.speed
        _uiState.value = _uiState.value.copy(
            activeDirection = direction,
            emergencyStopTriggered = false
        )
        val cmd = RoverProtocol.createMotorCommand(direction, speed)
        bluetoothService.sendCommand(cmd)
    }

    /**
     * Stop rover motion: "M,S,0\n"
     */
    fun stopDriving() {
        _uiState.value = _uiState.value.copy(activeDirection = DriveDirection.STOP)
        val cmd = RoverProtocol.createStopCommand()
        bluetoothService.sendCommand(cmd)
    }

    /**
     * Emergency STOP: immediately halts all drive motors
     */
    fun emergencyStop() {
        _uiState.value = _uiState.value.copy(
            activeDirection = DriveDirection.STOP,
            emergencyStopTriggered = true
        )
        val cmd = RoverProtocol.createStopCommand()
        bluetoothService.sendCommand(cmd)
        bluetoothService.addLog(LogType.INFO, "EMERGENCY STOP TRIGGERED")
    }

    /**
     * Update individual robotic arm joint: sends "jointIndex,angle\n"
     * Throttles slider movements to ~40ms to keep serial connection responsive.
     */
    fun updateArmJoint(jointIndex: Int, targetAngle: Int) {
        val clampedAngle = targetAngle.coerceIn(0, 180)
        val updatedJoints = _uiState.value.armJoints.map { joint ->
            if (joint.index == jointIndex) joint.copy(angle = clampedAngle) else joint
        }
        _uiState.value = _uiState.value.copy(armJoints = updatedJoints)

        // Cancel pending job for this joint
        armSendJobs[jointIndex]?.cancel()

        val now = System.currentTimeMillis()
        if (now - lastArmSendTime >= 40L) {
            lastArmSendTime = now
            val cmd = RoverProtocol.createArmCommand(jointIndex, clampedAngle)
            bluetoothService.sendCommand(cmd)
        } else {
            // Schedule debounced final send
            armSendJobs[jointIndex] = viewModelScope.launch {
                delay(45L)
                lastArmSendTime = System.currentTimeMillis()
                val cmd = RoverProtocol.createArmCommand(jointIndex, clampedAngle)
                bluetoothService.sendCommand(cmd)
            }
        }
    }

    fun stepJointAngle(jointIndex: Int, delta: Int) {
        val current = _uiState.value.armJoints.firstOrNull { it.index == jointIndex }?.angle ?: 90
        updateArmJoint(jointIndex, current + delta)
    }

    /**
     * Apply robotic arm preset (all 6 joints sequentially)
     */
    fun applyPreset(preset: ArmPreset) {
        if (preset.angles.size != 6) return
        val newJoints = _uiState.value.armJoints.mapIndexed { index, joint ->
            joint.copy(angle = preset.angles.getOrElse(index) { 90 })
        }
        _uiState.value = _uiState.value.copy(armJoints = newJoints)

        viewModelScope.launch {
            preset.angles.forEachIndexed { index, angle ->
                val cmd = RoverProtocol.createArmCommand(index, angle)
                bluetoothService.sendCommand(cmd)
                delay(35L) // Stagger slightly for servo controller
            }
            bluetoothService.addLog(LogType.INFO, "Applied Arm Preset: ${preset.name}")
        }
    }

    fun connectDevice(device: BluetoothDeviceInfo) {
        bluetoothService.connect(device)
    }

    fun disconnectDevice() {
        stopDriving()
        bluetoothService.disconnect()
    }

    fun startScan() {
        bluetoothService.startDiscovery()
    }

    fun stopScan() {
        bluetoothService.stopDiscovery()
    }

    fun refreshPaired() {
        bluetoothService.refreshPairedDevices()
    }

    fun toggleSimulation(enabled: Boolean) {
        bluetoothService.setSimulationMode(enabled)
    }

    fun sendCustomTerminalCommand(cmd: String) {
        if (cmd.isNotBlank()) {
            bluetoothService.sendCommand(cmd)
        }
    }

    fun clearTerminalLogs() {
        bluetoothService.clearLogs()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothService.destroy()
    }
}
