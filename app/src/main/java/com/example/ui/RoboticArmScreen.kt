package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.ArmJoint
import com.example.model.ArmPreset
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassPill
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldTelem
import com.example.viewmodel.DEFAULT_ARM_PRESETS
import com.example.viewmodel.RoverViewModel

@Composable
fun RoboticArmScreen(
    viewModel: RoverViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()
    val liquidTheme by viewModel.liquidTheme.collectAsState()
    val themeAccent = Color(liquidTheme.accentColor)

    var showSavePresetDialog by remember { mutableStateOf(false) }

    // Combine default and user-created custom presets
    val allPresets = DEFAULT_ARM_PRESETS + customPresets

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("robotic_arm_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Arm Hardware Liquid Glass Header
        item {
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassPill(
                            accentColor = themeAccent,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrecisionManufacturing,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PCA9685 6-Axis Arm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Format: jointIndex,angle\\n (0° - 180°)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    GlassButton(
                        onClick = { viewModel.applyPreset(DEFAULT_ARM_PRESETS[0]) },
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = Color.White.copy(alpha = 0.12f),
                        activeAccent = themeAccent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset 90°", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Tactical & Custom Presets Section with Add Feature
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Poses & Custom Presets",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (customPresets.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            GlassPill(accentColor = themeAccent, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${customPresets.size} custom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // "+ Add Preset" Glass Button
                    GlassButton(
                        onClick = { showSavePresetDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = themeAccent.copy(alpha = 0.20f),
                        activeAccent = themeAccent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Add Preset",
                                tint = themeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Save Pose",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Presets Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allPresets.forEach { preset ->
                        PresetGlassPill(
                            preset = preset,
                            accentColor = themeAccent,
                            onSelect = { viewModel.applyPreset(preset) },
                            onDelete = if (preset.isCustom) {
                                { viewModel.deleteCustomPreset(preset.id) }
                            } else null
                        )
                    }
                }
            }
        }

        // 6 Joint Control Cards in iOS Liquid Glass style
        items(uiState.armJoints, key = { it.index }) { joint ->
            JointGlassControlCard(
                joint = joint,
                themeAccent = themeAccent,
                onAngleChange = { angle -> viewModel.updateArmJoint(joint.index, angle) },
                onStepChange = { delta -> viewModel.stepJointAngle(joint.index, delta) }
            )
        }
    }

    // Modal Dialog to Save Custom Preset
    if (showSavePresetDialog) {
        SaveCustomPresetDialog(
            currentJoints = uiState.armJoints,
            accentColor = themeAccent,
            onDismiss = { showSavePresetDialog = false },
            onSave = { name, desc ->
                viewModel.saveCurrentPoseAsPreset(name, desc)
                showSavePresetDialog = false
            }
        )
    }
}

@Composable
fun PresetGlassPill(
    preset: ArmPreset,
    accentColor: Color,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    GlassBox(
        modifier = Modifier.testTag("preset_${preset.name.replace(" ", "_")}"),
        backgroundColor = if (preset.isCustom) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f),
        borderColor = if (preset.isCustom) accentColor else Color.White,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = if (onDelete != null) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                onClick = onSelect,
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color.Transparent
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (preset.isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.25f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "CUSTOM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                    Text(
                        text = preset.description,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            if (onDelete != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete Preset",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun JointGlassControlCard(
    joint: ArmJoint,
    themeAccent: Color,
    onAngleChange: (Int) -> Unit,
    onStepChange: (Int) -> Unit
) {
    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("joint_card_${joint.index}"),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Joint title and Degree Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassPill(
                        accentColor = themeAccent,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "${joint.index}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = themeAccent,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Joint ${joint.index}: ${joint.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = joint.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Numeric Degree readout pill
                GlassPill(
                    accentColor = themeAccent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${joint.angle}°",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = themeAccent,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specular slider
            Slider(
                value = joint.angle.toFloat(),
                onValueChange = { onAngleChange(it.toInt()) },
                valueRange = joint.minAngle.toFloat()..joint.maxAngle.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = themeAccent,
                    activeTrackColor = themeAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("joint_slider_${joint.index}")
            )

            // Fine-Tuning Steppers & Fast Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steppers: -5, -1, +1, +5
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassStepButton(text = "-5°", onClick = { onStepChange(-5) })
                    GlassStepButton(text = "-1°", onClick = { onStepChange(-1) })
                    GlassStepButton(text = "+1°", onClick = { onStepChange(1) })
                    GlassStepButton(text = "+5°", onClick = { onStepChange(5) })
                }

                // If Gripper Claw (Joint 5), provide quick Clamp / Open buttons
                if (joint.index == 5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassActionButton(
                            label = "Clamp",
                            icon = Icons.Default.Lock,
                            onClick = { onAngleChange(15) }
                        )
                        GlassActionButton(
                            label = "Open",
                            icon = Icons.Default.LockOpen,
                            onClick = { onAngleChange(170) }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassAnglePreset(label = "0°", target = 0, onAngleChange = onAngleChange)
                        GlassAnglePreset(label = "90°", target = 90, onAngleChange = onAngleChange)
                        GlassAnglePreset(label = "180°", target = 180, onAngleChange = onAngleChange)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassStepButton(text: String, onClick: () -> Unit) {
    GlassButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GlassAnglePreset(label: String, target: Int, onAngleChange: (Int) -> Unit) {
    GlassButton(
        onClick = { onAngleChange(target) },
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White.copy(alpha = 0.08f)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GlassActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    GlassButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SaveCustomPresetDialog(
    currentJoints: List<ArmJoint>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    var presetDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Custom Arm Preset",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Record current 6-joint servo configuration to instant-recall at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Current Angles Preview Badge
                GlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = Color.White.copy(alpha = 0.05f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Target Servo Angles:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentJoints.joinToString(" | ") { "J${it.index}: ${it.angle}°" },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }

                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("e.g. Ground Scoop, Inspection Pose", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_preset_name_input")
                )

                OutlinedTextField(
                    value = presetDesc,
                    onValueChange = { presetDesc = it },
                    label = { Text("Description (Optional)", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("Brief notes on this robotic pose", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(presetName, presetDesc) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_preset_confirm_button")
            ) {
                Text("Save Preset", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            GlassButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f)
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    )
}
