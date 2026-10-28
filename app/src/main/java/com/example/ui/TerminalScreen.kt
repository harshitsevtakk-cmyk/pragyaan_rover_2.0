package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CommandLog
import com.example.model.LogType
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldTelem
import com.example.viewmodel.RoverViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalScreen(
    viewModel: RoverViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.commandLogs.collectAsState()
    val liquidTheme by viewModel.liquidTheme.collectAsState()
    val themeAccent = Color(liquidTheme.accentColor)

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when logs change
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("terminal_screen")
    ) {
        // Terminal Glass Header
        GlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = themeAccent,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Serial Glass Terminal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Live RFCOMM buffer (${logs.size} lines)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearTerminalLogs() },
                    modifier = Modifier.testTag("clear_terminal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Logs",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Command Glass Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "M,S,0" to "Stop",
                "M,F,180" to "Drive Fwd",
                "M,B,140" to "Reverse",
                "0,90" to "Center Base",
                "5,180" to "Open Claw",
                "5,15" to "Grip Claw"
            ).forEach { (cmd, label) ->
                GlassButton(
                    onClick = { inputCommand = cmd },
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "$cmd ($label)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Console Output Log View in Frosted Glass
        GlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = Color.Black.copy(alpha = 0.45f)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Serial terminal idle.\nConnect rover or send a command to see traffic.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        TerminalGlassLogRow(log = log, themeAccent = themeAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Command Glass Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                placeholder = {
                    Text(
                        text = "Enter raw command (e.g. M,F,200)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            GlassButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.sendCustomTerminalCommand(inputCommand)
                        inputCommand = ""
                    }
                },
                shape = RoundedCornerShape(14.dp),
                backgroundColor = themeAccent.copy(alpha = 0.35f),
                activeAccent = themeAccent,
                modifier = Modifier
                    .height(54.dp)
                    .testTag("terminal_send_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = themeAccent
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalGlassLogRow(log: CommandLog, themeAccent: Color) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(log.timestamp))

    val (tag, tagColor, textColor) = when (log.type) {
        LogType.TX -> Triple("TX ->", themeAccent, Color(0xFFF1F5F9))
        LogType.RX -> Triple("RX <-", EmeraldTelem, Color(0xFFA7F3D0))
        LogType.INFO -> Triple("INFO", AmberWarning, Color(0xFFFDE68A))
        LogType.ERROR -> Triple("ERR ", CrimsonAlert, Color(0xFFFCA5A5))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = formattedTime,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.width(84.dp)
        )
        Text(
            text = tag,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = tagColor,
            modifier = Modifier.width(44.dp)
        )
        Text(
            text = log.message,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}
