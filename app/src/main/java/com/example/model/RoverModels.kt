package com.example.model

import java.util.UUID

/**
 * Motor drive directions matching the Arduino protocol:
 * 'F' = Forward
 * 'B' = Backward
 * 'L' = Left
 * 'R' = Right
 * 'S' = Stop
 */
enum class DriveDirection(val code: Char, val label: String) {
    FORWARD('F', "Forward"),
    BACKWARD('B', "Backward"),
    LEFT('L', "Left"),
    RIGHT('R', "Right"),
    STOP('S', "Stop");

    companion object {
        fun fromCode(code: Char): DriveDirection {
            return entries.firstOrNull { it.code == code } ?: STOP
        }
    }
}

/**
 * Incoming parsed telemetry packet from Arduino:
 * "D,temperature,humidity,soilMoisturePercent,mq2GasValue\n"
 */
data class TelemetryData(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val soilMoisturePercent: Int = 0,
    val mq2GasValue: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val rawPacket: String = "",
    val isValid: Boolean = false
) {
    val gasStatus: GasSafetyLevel
        get() = when {
            mq2GasValue > 350 -> GasSafetyLevel.DANGER
            mq2GasValue > 200 -> GasSafetyLevel.WARNING
            else -> GasSafetyLevel.NORMAL
        }

    val soilStatus: String
        get() = when {
            soilMoisturePercent >= 70 -> "Wet"
            soilMoisturePercent >= 35 -> "Moist"
            else -> "Dry"
        }
}

enum class GasSafetyLevel(val label: String) {
    NORMAL("Normal"),
    WARNING("Elevated"),
    DANGER("Gas Alert!")
}

/**
 * Robotic Arm Joint configuration (PCA9685 6-Axis Arm):
 * Joint index 0 to 5, angle 0 to 180 degrees.
 */
data class ArmJoint(
    val index: Int,
    val name: String,
    val role: String,
    val angle: Int = 90,
    val minAngle: Int = 0,
    val maxAngle: Int = 180
)

/**
 * Robotic arm postures including both built-in tactical presets
 * and user-created custom poses.
 */
data class ArmPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val angles: List<Int>, // 6 joint angles (0 to 5)
    val isCustom: Boolean = false
)

/**
 * Liquid Glass background theme colors with rich luminous glows
 */
enum class LiquidGlassTheme(
    val title: String,
    val topColor: Long,
    val midColor: Long,
    val bottomColor: Long,
    val accentColor: Long,
    val orbColor1: Long,
    val orbColor2: Long
) {
    AURORA_CYAN(
        title = "Aurora Cyan",
        topColor = 0xFF080D1A,
        midColor = 0xFF0D182E,
        bottomColor = 0xFF050912,
        accentColor = 0xFF00E5FF,
        orbColor1 = 0x5500E5FF,
        orbColor2 = 0x443B82F6
    ),
    MIDNIGHT_NEBULA(
        title = "Midnight Nebula",
        topColor = 0xFF0F0B1E,
        midColor = 0xFF1D1236,
        bottomColor = 0xFF080612,
        accentColor = 0xFFD946EF,
        orbColor1 = 0x55D946EF,
        orbColor2 = 0x448B5CF6
    ),
    CYBER_EMERALD(
        title = "Cyber Emerald",
        topColor = 0xFF061410,
        midColor = 0xFF0C241C,
        bottomColor = 0xFF040D0A,
        accentColor = 0xFF10B981,
        orbColor1 = 0x5510B981,
        orbColor2 = 0x4406B6D4
    ),
    SOLAR_FLARE(
        title = "Solar Flare",
        topColor = 0xFF160E08,
        midColor = 0xFF2B160A,
        bottomColor = 0xFF0E0704,
        accentColor = 0xFFF59E0B,
        orbColor1 = 0x55F59E0B,
        orbColor2 = 0x44EF4444
    )
}

/**
 * Bluetooth device representation for pairing and connection.
 */
data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean = true,
    val isHc05Likely: Boolean = name.contains("HC-05", ignoreCase = true) ||
            name.contains("HC-06", ignoreCase = true) ||
            name.contains("ROVER", ignoreCase = true) ||
            name.contains("ARDUINO", ignoreCase = true)
)

/**
 * Connection states for Bluetooth Classic SPP.
 */
sealed interface BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState
    data class Connecting(val deviceName: String, val address: String) : BluetoothConnectionState
    data class Connected(val deviceName: String, val address: String) : BluetoothConnectionState
    data class Error(val message: String) : BluetoothConnectionState
}

/**
 * Command log entry for the live Serial Terminal monitor.
 */
enum class LogType {
    TX, // Transmitted command
    RX, // Received telemetry / message
    INFO, // System status
    ERROR // Connection error
}

data class CommandLog(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val message: String
)
