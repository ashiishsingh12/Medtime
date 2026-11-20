package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.TimeSlot
import com.example.model.UserProfile
import com.example.model.formatTime12h
import com.example.ui.components.MedTimePickerDialog
import com.example.ui.components.rememberSlotVisualStyle

@Composable
fun SettingsDialog(
    userProfile: UserProfile,
    hasNotificationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSaveSettings: (
        displayName: String,
        morningH: Int,
        morningM: Int,
        afternoonH: Int,
        afternoonM: Int,
        eveningH: Int,
        eveningM: Int,
        nightH: Int,
        nightM: Int
    ) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    var displayName by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var morningHour by remember(userProfile) { mutableIntStateOf(userProfile.morningHour) }
    var morningMinute by remember(userProfile) { mutableIntStateOf(userProfile.morningMinute) }
    var afternoonHour by remember(userProfile) { mutableIntStateOf(userProfile.afternoonHour) }
    var afternoonMinute by remember(userProfile) { mutableIntStateOf(userProfile.afternoonMinute) }
    var eveningHour by remember(userProfile) { mutableIntStateOf(userProfile.eveningHour) }
    var eveningMinute by remember(userProfile) { mutableIntStateOf(userProfile.eveningMinute) }
    var nightHour by remember(userProfile) { mutableIntStateOf(userProfile.nightHour) }
    var nightMinute by remember(userProfile) { mutableIntStateOf(userProfile.nightMinute) }

    var activePickerSlot by remember { mutableStateOf<TimeSlot?>(null) }

    activePickerSlot?.let { slot ->
        val (h, m) = when (slot) {
            TimeSlot.MORNING -> morningHour to morningMinute
            TimeSlot.AFTERNOON -> afternoonHour to afternoonMinute
            TimeSlot.EVENING -> eveningHour to eveningMinute
            TimeSlot.NIGHT -> nightHour to nightMinute
        }
        MedTimePickerDialog(
            title = "Default ${slot.displayName} Time",
            initialHour = h,
            initialMinute = m,
            onDismiss = { activePickerSlot = null },
            onConfirm = { newH, newM ->
                when (slot) {
                    TimeSlot.MORNING -> {
                        morningHour = newH
                        morningMinute = newM
                    }
                    TimeSlot.AFTERNOON -> {
                        afternoonHour = newH
                        afternoonMinute = newM
                    }
                    TimeSlot.EVENING -> {
                        eveningHour = newH
                        eveningMinute = newM
                    }
                    TimeSlot.NIGHT -> {
                        nightHour = newH
                        nightMinute = newM
                    }
                }
                activePickerSlot = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Patient Profile & Defaults",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 60) displayName = it },
                    label = { Text("Patient Name (Optional)") },
                    placeholder = { Text("Patient") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patient_name_input")
                )

                Text(
                    text = "Default Reminder Times",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                DefaultTimeRow(
                    slot = TimeSlot.MORNING,
                    hour = morningHour,
                    minute = morningMinute,
                    onClick = { activePickerSlot = TimeSlot.MORNING }
                )
                DefaultTimeRow(
                    slot = TimeSlot.AFTERNOON,
                    hour = afternoonHour,
                    minute = afternoonMinute,
                    onClick = { activePickerSlot = TimeSlot.AFTERNOON }
                )
                DefaultTimeRow(
                    slot = TimeSlot.EVENING,
                    hour = eveningHour,
                    minute = eveningMinute,
                    onClick = { activePickerSlot = TimeSlot.EVENING }
                )
                DefaultTimeRow(
                    slot = TimeSlot.NIGHT,
                    hour = nightHour,
                    minute = nightMinute,
                    onClick = { activePickerSlot = TimeSlot.NIGHT }
                )

                Button(
                    onClick = {
                        onSaveSettings(
                            displayName,
                            morningHour,
                            morningMinute,
                            afternoonHour,
                            afternoonMinute,
                            eveningHour,
                            eveningMinute,
                            nightHour,
                            nightMinute
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_default_times_button")
                ) {
                    Text(if (isSaving) "Saving..." else "Save Settings")
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alert Notifications",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (hasNotificationPermission) "Enabled" else "Permission needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!hasNotificationPermission) {
                        TextButton(onClick = onRequestNotificationPermission) {
                            Text("Enable")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Notifications enabled",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!canScheduleExactAlarms) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarm Timing",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Allow exact schedule timing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onRequestExactAlarmPermission) {
                            Text("Allow")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun DefaultTimeRow(
    slot: TimeSlot,
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    val style = rememberSlotVisualStyle(slot)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = style.container.copy(alpha = 0.55f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = slot.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = style.accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime12h(hour, minute),
                    style = MaterialTheme.typography.labelMedium,
                    color = style.accent
                )
            }
        }
    }
}
