package com.example.protocol

import com.example.model.DriveDirection
import com.example.model.TelemetryData

/**
 * Encapsulates the communication protocol for the Arduino 6WD Rover + PCA9685 Arm.
 */
object RoverProtocol {

    // Standard Bluetooth Serial Port Profile (SPP) UUID
    const val SPP_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"

    /**
     * Motor command format: "M,direction,speed\n"
     * Directions: 'F', 'B', 'L', 'R', 'S'
     * Speed: 0 to 255
     */
    fun createMotorCommand(direction: DriveDirection, speed: Int): String {
        val clampedSpeed = speed.coerceIn(0, 255)
        return "M,${direction.code},$clampedSpeed\n"
    }

    /**
     * Stop motor command: "M,S,0\n"
     */
    fun createStopCommand(): String {
        return "M,S,0\n"
    }

    /**
     * Robotic arm command format: "jointIndex,angle\n"
     * jointIndex: 0 to 5
     * angle: 0 to 180
     */
    fun createArmCommand(jointIndex: Int, angle: Int): String {
        val clampedIndex = jointIndex.coerceIn(0, 5)
        val clampedAngle = angle.coerceIn(0, 180)
        return "$clampedIndex,$clampedAngle\n"
    }

    /**
     * Parses incoming telemetry string: "D,temperature,humidity,soilMoisturePercent,mq2GasValue\n"
     * Returns null if packet does not match format.
     */
    fun parseTelemetry(line: String): TelemetryData? {
        val cleaned = line.trim()
        if (!cleaned.startsWith("D,")) return null

        val tokens = cleaned.split(",")
        if (tokens.size < 5) return null

        return try {
            val temp = tokens[1].trim().toFloat()
            val hum = tokens[2].trim().toFloat()
            val soil = tokens[3].trim().toInt()
            val gas = tokens[4].trim().toInt()

            TelemetryData(
                temperature = temp,
                humidity = hum,
                soilMoisturePercent = soil,
                mq2GasValue = gas,
                timestamp = System.currentTimeMillis(),
                rawPacket = cleaned,
                isValid = true
            )
        } catch (_: Exception) {
            null
        }
    }
}
