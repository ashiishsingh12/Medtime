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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AdminIdentity
import com.example.model.TimeSlot
import com.example.model.UserAccount
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.formatEpochDmy
import com.example.model.formatTime12h
import com.example.ui.components.MedTimePickerDialog
import com.example.ui.components.rememberSlotVisualStyle

@Composable
fun SettingsDialog(
    userProfile: UserProfile,
    loggedInUser: UserAccount?,
    adminUsers: List<UserAccount>,
    sessionExpiresAtMillis: Long,
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
    onRequestExactAlarmPermission: () -> Unit,
    onUpdateUserRole: (userId: String, newRole: UserRole) -> Unit,
    onDeleteUser: (userId: String) -> Unit,
    onLogout: () -> Unit
) {
    var showAdminPanelDialog by remember { mutableStateOf(false) }
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

    if (showAdminPanelDialog && loggedInUser?.isAdmin == true) {
        AdminUsersDialog(
            currentAdminId = loggedInUser.id,
            users = adminUsers,
            onDismiss = { showAdminPanelDialog = false },
            onToggleRole = onUpdateUserRole,
            onDeleteUser = onDeleteUser
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

                if (loggedInUser != null) {
                    HorizontalDivider()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (loggedInUser.isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = loggedInUser.fullName.ifBlank { loggedInUser.email },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (loggedInUser.isAdmin) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("settings_admin_badge")
                                    ) {
                                        Text(
                                            text = "ADMIN",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${loggedInUser.email} • ${loggedInUser.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val validUntil = formatEpochDmy(sessionExpiresAtMillis)
                            if (validUntil.isNotBlank()) {
                                Text(
                                    text = "Logged in until $validUntil (90-day session)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (loggedInUser.isAdmin) {
                        OutlinedButton(
                            onClick = { showAdminPanelDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_admin_panel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Admin Panel (${adminUsers.size} Users)")
                        }
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out")
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
private fun AdminUsersDialog(
    currentAdminId: String,
    users: List<UserAccount>,
    onDismiss: () -> Unit,
    onToggleRole: (userId: String, newRole: UserRole) -> Unit,
    onDeleteUser: (userId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Admin Panel • Users")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Registered accounts (${users.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                users.forEach { user ->
                    val isDesignated = AdminIdentity.isDesignatedAdmin(user.email, user.phone)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (user.isAdmin) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (user.isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (user.isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = if (user.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = user.fullName.ifBlank { user.email },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Surface(
                                    color = if (user.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = user.role.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.isAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Phone: ${user.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val location = listOf(user.city, user.state).filter { it.isNotBlank() }.joinToString(", ")
                            val details = listOfNotNull(
                                user.gender.takeIf { it.isNotBlank() },
                                user.dob.takeIf { it.isNotBlank() }?.let { "DOB $it" },
                                location.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (details.isNotBlank()) {
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isDesignated && user.id != currentAdminId) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            onToggleRole(
                                                user.id,
                                                if (user.isAdmin) UserRole.USER else UserRole.ADMIN
                                            )
                                        }
                                    ) {
                                        Text(if (user.isAdmin) "Make User" else "Make Admin")
                                    }
                                    IconButton(onClick = { onDeleteUser(user.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove user",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
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
