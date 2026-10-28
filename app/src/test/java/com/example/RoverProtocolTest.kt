package com.example

import com.example.model.DriveDirection
import com.example.model.GasSafetyLevel
import com.example.protocol.RoverProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoverProtocolTest {

    @Test
    fun testMotorCommandFormatting() {
        val forwardCmd = RoverProtocol.createMotorCommand(DriveDirection.FORWARD, 200)
        assertEquals("M,F,200\n", forwardCmd)

        val backwardCmd = RoverProtocol.createMotorCommand(DriveDirection.BACKWARD, 140)
        assertEquals("M,B,140\n", backwardCmd)

        val leftCmd = RoverProtocol.createMotorCommand(DriveDirection.LEFT, 180)
        assertEquals("M,L,180\n", leftCmd)

        val rightCmd = RoverProtocol.createMotorCommand(DriveDirection.RIGHT, 180)
        assertEquals("M,R,180\n", rightCmd)

        val stopCmd = RoverProtocol.createStopCommand()
        assertEquals("M,S,0\n", stopCmd)
    }

    @Test
    fun testMotorSpeedClamping() {
        val overSpeed = RoverProtocol.createMotorCommand(DriveDirection.FORWARD, 350)
        assertEquals("M,F,255\n", overSpeed)

        val underSpeed = RoverProtocol.createMotorCommand(DriveDirection.FORWARD, -50)
        assertEquals("M,F,0\n", underSpeed)
    }

    @Test
    fun testArmCommandFormatting() {
        val baseJoint = RoverProtocol.createArmCommand(0, 90)
        assertEquals("0,90\n", baseJoint)

        val clawJoint = RoverProtocol.createArmCommand(5, 180)
        assertEquals("5,180\n", clawJoint)

        // Clamping angle to 0..180
        val clampedOver = RoverProtocol.createArmCommand(2, 220)
        assertEquals("2,180\n", clampedOver)

        val clampedUnder = RoverProtocol.createArmCommand(3, -20)
        assertEquals("3,0\n", clampedUnder)
    }

    @Test
    fun testTelemetryParsing() {
        val raw = "D,24.5,60.2,45,180\n"
        val telemetry = RoverProtocol.parseTelemetry(raw)

        assertNotNull(telemetry)
        telemetry!!
        assertTrue(telemetry.isValid)
        assertEquals(24.5f, telemetry.temperature, 0.001f)
        assertEquals(60.2f, telemetry.humidity, 0.001f)
        assertEquals(45, telemetry.soilMoisturePercent)
        assertEquals(180, telemetry.mq2GasValue)
        assertEquals(GasSafetyLevel.NORMAL, telemetry.gasStatus)
        assertEquals("Moist", telemetry.soilStatus)
    }

    @Test
    fun testTelemetryGasAlertLevels() {
        val normal = RoverProtocol.parseTelemetry("D,22.0,50.0,30,120\n")
        assertEquals(GasSafetyLevel.NORMAL, normal?.gasStatus)

        val warning = RoverProtocol.parseTelemetry("D,28.0,40.0,20,240\n")
        assertEquals(GasSafetyLevel.WARNING, warning?.gasStatus)

        val danger = RoverProtocol.parseTelemetry("D,35.0,30.0,10,420\n")
        assertEquals(GasSafetyLevel.DANGER, danger?.gasStatus)
    }

    @Test
    fun testMalformedTelemetryRejection() {
        assertNull(RoverProtocol.parseTelemetry("INVALID_PACKET"))
        assertNull(RoverProtocol.parseTelemetry("D,24.5,60.2")) // Missing fields
        assertNull(RoverProtocol.parseTelemetry("M,F,200\n")) // Motor cmd, not telemetry
        assertNull(RoverProtocol.parseTelemetry("D,not_a_float,60.2,45,180\n"))
    }
}
