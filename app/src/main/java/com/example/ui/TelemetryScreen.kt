package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GasSafetyLevel
import com.example.model.TelemetryData
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldTelem
import com.example.viewmodel.RoverViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelemetryScreen(
    viewModel: RoverViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val liquidTheme by viewModel.liquidTheme.collectAsState()
    val themeAccent = Color(liquidTheme.accentColor)

    val telemetry = uiState.latestTelemetry
    val history = uiState.telemetryHistory

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("telemetry_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gas Hazard Alert Banner (if danger or warning)
        item {
            AnimatedVisibility(visible = telemetry.gasStatus != GasSafetyLevel.NORMAL) {
                GasAlertGlassCard(status = telemetry.gasStatus, gasValue = telemetry.mq2GasValue)
            }
        }

        // Overview / Header Card
        item {
            TelemetryOverviewGlassHeader(
                packetCount = history.size,
                lastUpdate = telemetry.timestamp,
                rawPacket = telemetry.rawPacket,
                isValid = telemetry.isValid,
                themeAccent = themeAccent
            )
        }

        // 2x2 Sensor Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Temperature Card
                SensorMetricGlassCard(
                    modifier = Modifier.weight(1f),
                    title = "Temperature",
                    value = if (telemetry.isValid) "%.1f°C".format(telemetry.temperature) else "--",
                    subtitle = when {
                        !telemetry.isValid -> "Awaiting data"
                        telemetry.temperature > 35f -> "High Heat"
                        telemetry.temperature < 15f -> "Chilly"
                        else -> "Optimal"
                    },
                    icon = Icons.Default.Thermostat,
                    accentColor = when {
                        telemetry.temperature > 35f -> CrimsonAlert
                        telemetry.temperature > 28f -> AmberWarning
                        else -> themeAccent
                    },
                    progress = if (telemetry.isValid) (telemetry.temperature / 50f).coerceIn(0f, 1f) else 0f
                )

                // Humidity Card
                SensorMetricGlassCard(
                    modifier = Modifier.weight(1f),
                    title = "Humidity",
                    value = if (telemetry.isValid) "%.1f%%".format(telemetry.humidity) else "--",
                    subtitle = when {
                        !telemetry.isValid -> "Awaiting data"
                        telemetry.humidity > 70f -> "High Moisture"
                        telemetry.humidity < 30f -> "Low Humidity"
                        else -> "Comfortable"
                    },
                    icon = Icons.Default.WaterDrop,
                    accentColor = Color(0xFF38BDF8),
                    progress = if (telemetry.isValid) (telemetry.humidity / 100f).coerceIn(0f, 1f) else 0f
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Soil Moisture Card
                SensorMetricGlassCard(
                    modifier = Modifier.weight(1f),
                    title = "Soil Moisture",
                    value = if (telemetry.isValid) "${telemetry.soilMoisturePercent}%" else "--",
                    subtitle = when {
                        !telemetry.isValid -> "Awaiting probe"
                        telemetry.soilMoisturePercent >= 70 -> "Wet / Saturated"
                        telemetry.soilMoisturePercent >= 35 -> "Moist Soil"
                        else -> "Dry Ground"
                    },
                    icon = Icons.Default.Grass,
                    accentColor = when {
                        telemetry.soilMoisturePercent >= 70 -> themeAccent
                        telemetry.soilMoisturePercent >= 35 -> EmeraldTelem
                        else -> AmberWarning
                    },
                    progress = if (telemetry.isValid) (telemetry.soilMoisturePercent / 100f).coerceIn(0f, 1f) else 0f
                )

                // MQ-2 Gas Sensor Card
                SensorMetricGlassCard(
                    modifier = Modifier.weight(1f),
                    title = "MQ-2 Gas",
                    value = if (telemetry.isValid) "${telemetry.mq2GasValue}" else "--",
                    subtitle = when (telemetry.gasStatus) {
                        GasSafetyLevel.DANGER -> "GAS HAZARD!"
                        GasSafetyLevel.WARNING -> "Elevated Vapor"
                        GasSafetyLevel.NORMAL -> "Safe Air"
                    },
                    icon = Icons.Default.Air,
                    accentColor = when (telemetry.gasStatus) {
                        GasSafetyLevel.DANGER -> CrimsonAlert
                        GasSafetyLevel.WARNING -> AmberWarning
                        GasSafetyLevel.NORMAL -> EmeraldTelem
                    },
                    progress = if (telemetry.isValid) (telemetry.mq2GasValue / 500f).coerceIn(0f, 1f) else 0f
                )
            }
        }

        // Live Sparkline Chart
        item {
            TelemetryHistoryGlassChartCard(history = history, themeAccent = themeAccent)
        }
    }
}

@Composable
fun TelemetryOverviewGlassHeader(
    packetCount: Int,
    lastUpdate: Long,
    rawPacket: String,
    isValid: Boolean,
    themeAccent: Color
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = if (isValid) timeFormat.format(Date(lastUpdate)) else "--:--:--"

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
                        accentColor = EmeraldTelem,
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = EmeraldTelem,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Live Environmental Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Arduino 'D,temp,hum,soil,gas' stream",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                GlassPill(
                    accentColor = if (isValid) EmeraldTelem else Color.Gray,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isValid) "LIVE $formattedTime" else "OFFLINE",
                        color = if (isValid) EmeraldTelem else Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (rawPacket.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                GlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = Color.Black.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RAW: $rawPacket",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = themeAccent
                        )
                        Text(
                            text = "$packetCount pkts",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorMetricGlassCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier.testTag("sensor_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                GlassPill(
                    accentColor = accentColor,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
fun GasAlertGlassCard(status: GasSafetyLevel, gasValue: Int) {
    val isDanger = status == GasSafetyLevel.DANGER
    val color = if (isDanger) CrimsonAlert else AmberWarning

    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = color.copy(alpha = 0.18f),
        borderColor = color,
        borderWidth = 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isDanger) "DANGER: HIGH GAS / SMOKE DETECTED!" else "CAUTION: ELEVATED GAS READING",
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "MQ-2 reading: $gasValue. Check rover proximity immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun TelemetryHistoryGlassChartCard(history: List<TelemetryData>, themeAccent: Color) {
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
                Text(
                    text = "Atmospheric Fluctuation Sparkline",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendGlass(color = themeAccent, label = "Temp")
                    ChartLegendGlass(color = EmeraldTelem, label = "Gas")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (history.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Collecting live sensor packets for trend graph...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(8.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val n = history.size

                    // Grid lines
                    for (i in 1..3) {
                        val y = h * (i.toFloat() / 4)
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    val tempPath = Path()
                    val gasPath = Path()

                    val minTemp = 15f
                    val maxTemp = 45f
                    val minGas = 50f
                    val maxGas = 500f

                    history.forEachIndexed { index, d ->
                        val x = (index.toFloat() / (n - 1)) * w
                        val normTemp = ((d.temperature - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
                        val yTemp = h - (normTemp * h)

                        val normGas = ((d.mq2GasValue - minGas) / (maxGas - minGas)).coerceIn(0f, 1f)
                        val yGas = h - (normGas * h)

                        if (index == 0) {
                            tempPath.moveTo(x, yTemp)
                            gasPath.moveTo(x, yGas)
                        } else {
                            tempPath.lineTo(x, yTemp)
                            gasPath.lineTo(x, yGas)
                        }
                    }

                    drawPath(
                        path = tempPath,
                        color = themeAccent,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = gasPath,
                        color = EmeraldTelem,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegendGlass(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
    }
}
