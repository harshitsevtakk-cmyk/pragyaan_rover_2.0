package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriveDirection
import com.example.model.TelemetryData
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldTelem
import com.example.viewmodel.DriveMode
import com.example.viewmodel.RoverViewModel

@Composable
fun DriveControlScreen(
    viewModel: RoverViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val liquidTheme by viewModel.liquidTheme.collectAsState()
    val themeAccent = Color(liquidTheme.accentColor)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("drive_control_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cockpit Telemetry Glass Ribbon
        item {
            CockpitGlassRibbon(
                direction = uiState.activeDirection,
                speed = uiState.speed,
                latestTelemetry = uiState.latestTelemetry,
                themeAccent = themeAccent
            )
        }

        // Global Speed Glass Card
        item {
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassPill(
                                accentColor = themeAccent,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = themeAccent,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Motor Thrust (PWM)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Glass Speed Pill
                        GlassPill(
                            accentColor = themeAccent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${uiState.speed} / 255",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = themeAccent,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = uiState.speed.toFloat(),
                        onValueChange = { viewModel.setSpeed(it.toInt()) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = themeAccent,
                            activeTrackColor = themeAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("global_speed_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Speed Glass Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Crawl (60)" to 60,
                            "Cruise (140)" to 140,
                            "Patrol (200)" to 200,
                            "Turbo (255)" to 255
                        ).forEach { (label, presetSpeed) ->
                            val isSelected = uiState.speed == presetSpeed
                            GlassButton(
                                onClick = { viewModel.setSpeed(presetSpeed) },
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (isSelected) themeAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                activeAccent = themeAccent,
                                isActive = isSelected,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("speed_preset_$presetSpeed")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) themeAccent else Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drive Mode Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Controls Response Mode:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isHold = uiState.driveMode == DriveMode.HOLD_TO_RUN
                    GlassButton(
                        onClick = { viewModel.setDriveMode(DriveMode.HOLD_TO_RUN) },
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isHold) themeAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        activeAccent = themeAccent,
                        isActive = isHold,
                        modifier = Modifier.testTag("mode_hold_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.PanTool, contentDescription = null, tint = if (isHold) themeAccent else Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hold to Run", fontSize = 11.sp, color = if (isHold) themeAccent else Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    val isToggle = uiState.driveMode == DriveMode.TOGGLE_RUN
                    GlassButton(
                        onClick = { viewModel.setDriveMode(DriveMode.TOGGLE_RUN) },
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isToggle) themeAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        activeAccent = themeAccent,
                        isActive = isToggle,
                        modifier = Modifier.testTag("mode_toggle_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = if (isToggle) themeAccent else Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Toggle Run", fontSize = 11.sp, color = if (isToggle) themeAccent else Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Directional Control Cockpit in Liquid Glass
        item {
            DirectionalGlassPad(
                activeDirection = uiState.activeDirection,
                driveMode = uiState.driveMode,
                themeAccent = themeAccent,
                onDirectionStart = { dir -> viewModel.startDriving(dir) },
                onDirectionStop = { viewModel.stopDriving() },
                onEmergencyStop = { viewModel.emergencyStop() }
            )
        }

        // 6WD Chassis Graphical Visualizer
        item {
            Chassis6WdGlassVisualizer(
                activeDirection = uiState.activeDirection,
                themeAccent = themeAccent
            )
        }
    }
}

@Composable
fun CockpitGlassRibbon(
    direction: DriveDirection,
    speed: Int,
    latestTelemetry: TelemetryData,
    themeAccent: Color
) {
    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (direction == DriveDirection.STOP) Color.Gray else EmeraldTelem)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DRIVE: ${direction.label.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (direction == DriveDirection.STOP) Color.White.copy(alpha = 0.6f) else EmeraldTelem
                    )
                    Text(
                        text = "M,${direction.code},${if (direction == DriveDirection.STOP) 0 else speed}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = themeAccent
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill(accentColor = themeAccent, shape = RoundedCornerShape(8.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("TEMP", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (latestTelemetry.isValid) "%.1f°C".format(latestTelemetry.temperature) else "--",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = themeAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                GlassPill(
                    accentColor = when (latestTelemetry.gasStatus) {
                        com.example.model.GasSafetyLevel.DANGER -> CrimsonAlert
                        com.example.model.GasSafetyLevel.WARNING -> AmberWarning
                        com.example.model.GasSafetyLevel.NORMAL -> EmeraldTelem
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("GAS", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Text(
                            text = if (latestTelemetry.isValid) "${latestTelemetry.mq2GasValue}" else "--",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = when (latestTelemetry.gasStatus) {
                                com.example.model.GasSafetyLevel.DANGER -> CrimsonAlert
                                com.example.model.GasSafetyLevel.WARNING -> AmberWarning
                                com.example.model.GasSafetyLevel.NORMAL -> EmeraldTelem
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DirectionalGlassPad(
    activeDirection: DriveDirection,
    driveMode: DriveMode,
    themeAccent: Color,
    onDirectionStart: (DriveDirection) -> Unit,
    onDirectionStop: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Forward Glass Button
            DirectionGlassButton(
                direction = DriveDirection.FORWARD,
                icon = Icons.Default.ArrowUpward,
                isActive = activeDirection == DriveDirection.FORWARD,
                driveMode = driveMode,
                themeAccent = themeAccent,
                onStart = { onDirectionStart(DriveDirection.FORWARD) },
                onStop = onDirectionStop,
                modifier = Modifier.size(width = 135.dp, height = 75.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Middle Row: Left, Emergency STOP, Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left
                DirectionGlassButton(
                    direction = DriveDirection.LEFT,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    isActive = activeDirection == DriveDirection.LEFT,
                    driveMode = driveMode,
                    themeAccent = themeAccent,
                    onStart = { onDirectionStart(DriveDirection.LEFT) },
                    onStop = onDirectionStop,
                    modifier = Modifier.size(width = 95.dp, height = 75.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Emergency STOP Liquid Glass Button
                EmergencyStopGlassButton(
                    isDriving = activeDirection != DriveDirection.STOP,
                    onClick = onEmergencyStop,
                    modifier = Modifier.size(86.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Right
                DirectionGlassButton(
                    direction = DriveDirection.RIGHT,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isActive = activeDirection == DriveDirection.RIGHT,
                    driveMode = driveMode,
                    themeAccent = themeAccent,
                    onStart = { onDirectionStart(DriveDirection.RIGHT) },
                    onStop = onDirectionStop,
                    modifier = Modifier.size(width = 95.dp, height = 75.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Backward Glass Button
            DirectionGlassButton(
                direction = DriveDirection.BACKWARD,
                icon = Icons.Default.ArrowDownward,
                isActive = activeDirection == DriveDirection.BACKWARD,
                driveMode = driveMode,
                themeAccent = themeAccent,
                onStart = { onDirectionStart(DriveDirection.BACKWARD) },
                onStop = onDirectionStop,
                modifier = Modifier.size(width = 135.dp, height = 75.dp)
            )
        }
    }
}

@Composable
fun DirectionGlassButton(
    direction: DriveDirection,
    icon: ImageVector,
    isActive: Boolean,
    driveMode: DriveMode,
    themeAccent: Color,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(targetValue = if (isActive) 0.94f else 1.0f, label = "button_scale")

    val bgBrush = if (isActive) {
        Brush.verticalGradient(
            listOf(themeAccent.copy(alpha = 0.5f), themeAccent.copy(alpha = 0.25f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f))
        )
    }

    val rimBrush = if (isActive) {
        Brush.verticalGradient(listOf(themeAccent, themeAccent.copy(alpha = 0.4f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.08f), Color.Transparent))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(1.dp, rimBrush, RoundedCornerShape(18.dp))
            .pointerInput(driveMode) {
                detectTapGestures(
                    onPress = {
                        if (driveMode == DriveMode.HOLD_TO_RUN) {
                            onStart()
                            tryAwaitRelease()
                            onStop()
                        } else {
                            if (isActive) onStop() else onStart()
                        }
                    }
                )
            }
            .testTag("direction_btn_${direction.code}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = direction.label,
                tint = if (isActive) themeAccent else Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = direction.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) themeAccent else Color.White
            )
        }
    }
}

@Composable
fun EmergencyStopGlassButton(
    isDriving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isDriving) {
        Brush.radialGradient(listOf(CrimsonAlert, CrimsonAlert.copy(alpha = 0.7f)))
    } else {
        Brush.verticalGradient(listOf(CrimsonAlert.copy(alpha = 0.25f), CrimsonAlert.copy(alpha = 0.08f)))
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.5.dp, CrimsonAlert, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .testTag("emergency_stop_button"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "STOP",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "STOP",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun Chassis6WdGlassVisualizer(
    activeDirection: DriveDirection,
    themeAccent: Color
) {
    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6WD Drivetrain Differential",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Arduino 6-Wheel Drive",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                val w = size.width
                val h = size.height

                val bodyWidth = w * 0.44f
                val bodyHeight = h * 0.65f
                val bodyLeft = (w - bodyWidth) / 2f
                val bodyTop = (h - bodyHeight) / 2f

                // Chassis glass body
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = if (activeDirection != DriveDirection.STOP) themeAccent else Color.White.copy(alpha = 0.25f),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 6 Wheels
                val wheelWidth = 14.dp.toPx()
                val wheelHeight = 22.dp.toPx()
                val leftX = bodyLeft - wheelWidth * 0.6f
                val rightX = bodyLeft + bodyWidth - wheelWidth * 0.4f

                val yPositions = listOf(
                    bodyTop + 4.dp.toPx(),
                    bodyTop + (bodyHeight - wheelHeight) / 2f,
                    bodyTop + bodyHeight - wheelHeight - 4.dp.toPx()
                )

                val leftActive = activeDirection == DriveDirection.FORWARD ||
                        activeDirection == DriveDirection.BACKWARD ||
                        activeDirection == DriveDirection.RIGHT
                val rightActive = activeDirection == DriveDirection.FORWARD ||
                        activeDirection == DriveDirection.BACKWARD ||
                        activeDirection == DriveDirection.LEFT

                // Draw Left Wheels
                yPositions.forEach { y ->
                    drawRoundRect(
                        color = if (leftActive) themeAccent else Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(leftX, y),
                        size = Size(wheelWidth, wheelHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                    )
                }

                // Draw Right Wheels
                yPositions.forEach { y ->
                    drawRoundRect(
                        color = if (rightActive) themeAccent else Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(rightX, y),
                        size = Size(wheelWidth, wheelHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                    )
                }
            }
        }
    }
}
