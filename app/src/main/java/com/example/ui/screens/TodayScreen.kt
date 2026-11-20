package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Medicine
import com.example.model.ScheduledDoseItem
import com.example.model.TimeSlot
import com.example.ui.components.iconVector
import com.example.ui.components.rememberSlotVisualStyle
import com.example.ui.theme.SuccessContainerDark
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.TimetableDashboardState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TodayScreen(
    state: TimetableDashboardState,
    hasNotificationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onSelectFilter: (TimeSlot?) -> Unit,
    onToggleDoseTaken: (ScheduledDoseItem) -> Unit,
    onMarkDoseSkipped: (ScheduledDoseItem) -> Unit,
    onEditMedicine: (Medicine) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredDoses = remember(state.todayDoses, state.selectedFilter) {
        if (state.selectedFilter == null) state.todayDoses
        else state.todayDoses.filter { it.slot == state.selectedFilter }
    }
    val groupedBySlot = remember(filteredDoses) {
        TimeSlot.entries.mapNotNull { slot ->
            val items = filteredDoses.filter { it.slot == slot }
            if (items.isEmpty()) null else slot to items
        }
    }
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentTimeMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val calendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }
    val nextDose = remember(state.todayDoses, calendar) {
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val pending = state.todayDoses.filter { !it.isTaken && !it.isSkipped }
        pending.firstOrNull { (it.hour * 60 + it.minute) >= nowMinutes } ?: pending.firstOrNull()
    }
    val todayLabel = remember(calendar) {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(calendar.time)
    }
    val greeting = remember(calendar) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AdherenceHeroCard(
                greeting = greeting,
                patientName = state.userProfile.displayName,
                dateLabel = todayLabel,
                takenCount = state.todayTakenCount,
                totalCount = state.todayTotalCount
            )
        }

        if (!hasNotificationPermission) {
            item {
                PermissionAlertBanner(
                    title = "Enable Medicine Alert Notifications",
                    subtitle = "Allow notifications so MedTime can ring and remind you at morning, afternoon, evening, and night times.",
                    actionLabel = "Allow Alerts",
                    onActionClick = onRequestNotificationPermission,
                    testTag = "enable_notifications_button"
                )
            }
        } else if (!canScheduleExactAlarms) {
            item {
                PermissionAlertBanner(
                    title = "Allow Exact Reminder Timing",
                    subtitle = "Grant Alarms & Reminders permission for pinpoint-accurate morning, afternoon, evening, and night alerts.",
                    actionLabel = "Allow Timing",
                    onActionClick = onRequestExactAlarmPermission,
                    testTag = "enable_exact_alarms_button"
                )
            }
        }

        if (state.medicines.isEmpty()) {
            item {
                TodaysNextDoseEmptyCard(userProfile = state.userProfile)
            }
        } else {
            item {
                TodaysNextDoseActiveCard(
                    nextDose = nextDose,
                    allDosesCount = state.todayDoses.size,
                    onMarkTaken = { dose -> onToggleDoseTaken(dose) },
                    onEditMedicine = onEditMedicine
                )
            }
            item {
                SlotFilterRow(
                    selectedSlot = state.selectedFilter,
                    onSelectFilter = onSelectFilter,
                    doses = state.todayDoses
                )
            }

            if (groupedBySlot.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (state.selectedFilter != null)
                                    "No active medicines scheduled for ${state.selectedFilter.displayName}"
                                else
                                    "All your medicines are currently paused",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap Timetable or Add Medicine to schedule doses for this slot.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                groupedBySlot.forEach { (slot, slotItems) ->
                    item(key = "header_${slot.name}") {
                        SlotSectionHeader(
                            slot = slot,
                            takenInSlot = slotItems.count { it.isTaken },
                            totalInSlot = slotItems.size
                        )
                    }
                    items(
                        items = slotItems,
                        key = { "${it.slot.name}_${it.medicine.id}" }
                    ) { doseItem ->
                        DoseScheduleCard(
                            item = doseItem,
                            onToggleTaken = { onToggleDoseTaken(doseItem) },
                            onSkip = { onMarkDoseSkipped(doseItem) },
                            onEdit = { onEditMedicine(doseItem.medicine) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdherenceHeroCard(
    greeting: String,
    patientName: String,
    dateLabel: String,
    takenCount: Int,
    totalCount: Int
) {
    val progressTarget = if (totalCount > 0) takenCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "adherenceProgress"
    )
    val firstName = patientName.trim().substringBefore(" ").ifBlank { "Patient" }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$greeting, $firstName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            totalCount == 0 -> "Set up your daily medicine timetable below"
                            takenCount == totalCount -> "All $totalCount doses taken today. Great care!"
                            else -> "$takenCount of $totalCount doses taken today"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
                        strokeWidth = 7.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = if (totalCount == 0) "0%" else "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionAlertBanner(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SlotFilterRow(
    selectedSlot: TimeSlot?,
    onSelectFilter: (TimeSlot?) -> Unit,
    doses: List<ScheduledDoseItem>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedSlot == null,
            onClick = { onSelectFilter(null) },
            label = { Text("All (${doses.size})") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_all")
        )
        TimeSlot.entries.forEach { slot ->
            val count = doses.count { it.slot == slot }
            val style = rememberSlotVisualStyle(slot)
            FilterChip(
                selected = selectedSlot == slot,
                onClick = { onSelectFilter(if (selectedSlot == slot) null else slot) },
                leadingIcon = {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = if (selectedSlot == slot) style.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("${slot.displayName} ($count)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = style.container,
                    selectedLabelColor = style.accent
                ),
                modifier = Modifier.testTag("filter_${slot.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun SlotSectionHeader(
    slot: TimeSlot,
    takenInSlot: Int,
    totalInSlot: Int
) {
    val style = rememberSlotVisualStyle(slot)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(style.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = slot.displayName,
                    tint = style.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${slot.displayName} • ${slot.localSubtitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Surface(
            color = style.container,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "$takenInSlot/$totalInSlot taken",
                style = MaterialTheme.typography.labelMedium,
                color = style.accent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DoseScheduleCard(
    item: ScheduledDoseItem,
    onToggleTaken: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit
) {
    val slotStyle = rememberSlotVisualStyle(item.slot)
    val dark = isSystemInDarkTheme()
    val takenBg by animateColorAsState(
        targetValue = if (item.isTaken) {
            if (dark) SuccessContainerDark else SuccessContainerLight
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "doseCardBg"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = takenBg),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isTaken) SuccessGreen.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("dose_card_${item.slot.name.lowercase()}_${item.medicine.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (item.isTaken) SuccessGreen.copy(alpha = 0.16f)
                            else slotStyle.container
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isTaken) Icons.Filled.CheckCircle else item.medicine.formEnum().iconVector(),
                        contentDescription = item.medicine.formEnum().displayName,
                        tint = if (item.isTaken) SuccessGreen else slotStyle.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isTaken) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${item.medicine.dosage} • ${item.medicine.formEnum().displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onToggleTaken,
                    shape = RoundedCornerShape(14.dp),
                    colors = if (item.isTaken) {
                        ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("take_dose_button_${item.slot.name.lowercase()}_${item.medicine.id}")
                ) {
                    if (item.isTaken) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Taken", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text("Take", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = slotStyle.container,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = slotStyle.accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = slotStyle.accent
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.mealRelation.shortTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!item.isTaken && !item.isSkipped) {
                    TextButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("skip_dose_button_${item.slot.name.lowercase()}_${item.medicine.id}")
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (item.isSkipped) {
                    Text(
                        text = "Skipped today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            AnimatedVisibility(visible = item.medicine.notes.isNotBlank()) {
                Text(
                    text = item.medicine.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TodaysNextDoseActiveCard(
    nextDose: ScheduledDoseItem?,
    allDosesCount: Int,
    onMarkTaken: (ScheduledDoseItem) -> Unit,
    onEditMedicine: (Medicine) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todays_next_dose_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Today's Next Dose",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (nextDose != null) {
                    val slotStyle = rememberSlotVisualStyle(nextDose.slot)
                    Surface(
                        color = slotStyle.container,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${nextDose.slot.displayName} • ${nextDose.formattedTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = slotStyle.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (nextDose != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditMedicine(nextDose.medicine) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nextDose.medicine.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${nextDose.medicine.dosage} • ${nextDose.mealRelation.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onMarkTaken(nextDose) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("next_dose_take_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take Now", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (allDosesCount > 0)
                            "All scheduled doses for today are complete."
                        else
                            "No active doses scheduled for today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaysNextDoseEmptyCard(userProfile: com.example.model.UserProfile) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todays_next_dose_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_schedule),
                contentDescription = "Empty medicine timetable illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Today's Next Dose",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "No Dose Yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Your next Morning, Afternoon, Evening, or Night medicine alert will appear here once you add your timetable below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val slotsWithTimes = listOf(
                    TimeSlot.MORNING to com.example.model.formatTime12h(
                        userProfile.morningHour,
                        userProfile.morningMinute
                    ),
                    TimeSlot.AFTERNOON to com.example.model.formatTime12h(
                        userProfile.afternoonHour,
                        userProfile.afternoonMinute
                    ),
                    TimeSlot.EVENING to com.example.model.formatTime12h(
                        userProfile.eveningHour,
                        userProfile.eveningMinute
                    ),
                    TimeSlot.NIGHT to com.example.model.formatTime12h(
                        userProfile.nightHour,
                        userProfile.nightMinute
                    )
                )
                slotsWithTimes.forEach { (slot, timeLabel) ->
                    val style = rememberSlotVisualStyle(slot)
                    Surface(
                        color = style.container,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = style.icon,
                                contentDescription = slot.displayName,
                                tint = style.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = slot.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = style.accent,
                                maxLines = 1
                            )
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
