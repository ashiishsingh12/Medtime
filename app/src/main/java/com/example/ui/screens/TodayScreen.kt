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
import androidx.compose.material.icons.filled.Person
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
import com.example.model.FamilyMember
import com.example.model.Medicine
import com.example.model.ScheduledDoseItem
import com.example.model.TimeSlot
import com.example.model.TodayDoseFilter
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
    onSelectFilter: (TodayDoseFilter) -> Unit,
    onToggleDoseTaken: (ScheduledDoseItem) -> Unit,
    onMarkDoseSkipped: (ScheduledDoseItem) -> Unit,
    onEditMedicine: (Medicine) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingDoses = remember(state.todayDoses) {
        state.todayDoses.filter { !it.isTaken }
    }
    val takenDoses = remember(state.todayDoses) {
        state.todayDoses.filter { it.isTaken }
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

    val isAllMembers = state.selectedMemberId == FamilyMember.ALL_ID
    val activeMemberLabel = if (isAllMembers) "All Family" else (state.selectedMember?.name ?: "Self")
    val selfCustomName = state.selectedMember?.name
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals("Self", ignoreCase = true) }
        ?: state.loggedInUser?.firstName?.trim()?.takeIf { it.isNotBlank() }
        ?: state.userProfile.displayName
            .trim()
            .substringBefore(" ")
            .takeIf {
                it.isNotBlank() &&
                    !it.equals("Patient", ignoreCase = true) &&
                    !it.equals("Self", ignoreCase = true)
            }
    val heroPatientName: String = when {
        isAllMembers -> "Family"
        state.selectedMemberId == FamilyMember.SELF_ID -> selfCustomName ?: "Patient"
        else -> state.selectedMember?.name?.trim()?.ifBlank { "Patient" } ?: "Patient"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdherenceHeroCard(
                greeting = greeting,
                patientName = heroPatientName,
                dateLabel = todayLabel,
                takenCount = state.todayTakenCount,
                totalCount = state.todayTotalCount
            )
        }

        if (!hasNotificationPermission) {
            item {
                PermissionAlertBanner(
                    title = "Enable Medicine Alert Notifications",
                    subtitle = "Allow notifications so MedTym can ring and remind you at morning, afternoon, evening, and night times.",
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
                TodaysNextDoseEmptyCard(
                    userProfile = state.userProfile,
                    activeMemberLabel = activeMemberLabel
                )
            }
        } else {
            item {
                TodaysNextDoseActiveCard(
                    nextDose = nextDose,
                    allDosesCount = state.todayDoses.size,
                    showMemberBadge = isAllMembers,
                    onMarkTaken = { dose -> onToggleDoseTaken(dose) },
                    onEditMedicine = onEditMedicine
                )
            }
            item {
                SlotFilterRow(
                    selectedFilter = state.selectedFilter,
                    onSelectFilter = onSelectFilter,
                    pendingDoses = pendingDoses,
                    takenCount = takenDoses.size
                )
            }

            when (val filter = state.selectedFilter) {
                is TodayDoseFilter.All -> {
                    if (pendingDoses.isEmpty()) {
                        item {
                            EmptyDoseFilterCard(
                                icon = if (takenDoses.isNotEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                                title = when {
                                    takenDoses.isNotEmpty() -> "All pending doses taken today!"
                                    state.medicines.any { it.active } -> "No doses scheduled for today"
                                    else -> "All your medicines are currently paused"
                                },
                                subtitle = when {
                                    takenDoses.isNotEmpty() ->
                                        "Tap the Taken tab above to view completed medicines."
                                    state.medicines.any { it.active } ->
                                        "Your medicines have upcoming or ended course dates. Open Timetable to review or extend dates."
                                    else ->
                                        "Tap Timetable or Add Medicine to schedule doses."
                                }
                            )
                        }
                    } else {
                        items(
                            items = pendingDoses,
                            key = { "pending_${it.slot.name}_${it.medicine.id}" }
                        ) { doseItem ->
                            DoseScheduleCard(
                                item = doseItem,
                                showMemberBadge = isAllMembers,
                                onToggleTaken = { onToggleDoseTaken(doseItem) },
                                onSkip = { onMarkDoseSkipped(doseItem) },
                                onEdit = { onEditMedicine(doseItem.medicine) }
                            )
                        }
                    }
                }

                is TodayDoseFilter.Slot -> {
                    val slot = filter.slot
                    val slotPending = pendingDoses.filter { it.slot == slot }
                    val slotTakenCount = takenDoses.count { it.slot == slot }

                    item(key = "header_${slot.name}") {
                        SlotSectionHeader(
                            slot = slot,
                            remainingInSlot = slotPending.size
                        )
                    }

                    if (slotPending.isEmpty()) {
                        item {
                            EmptyDoseFilterCard(
                                icon = if (slotTakenCount > 0) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                                title = if (slotTakenCount > 0) {
                                    "All ${slot.displayName} medicines taken"
                                } else {
                                    "No active medicines scheduled for ${slot.displayName}"
                                },
                                subtitle = if (slotTakenCount > 0) {
                                    "Completed ${slot.displayName} medicines have moved to the Taken tab."
                                } else {
                                    "Tap Timetable or Add Medicine to schedule doses for ${slot.displayName}."
                                }
                            )
                        }
                    } else {
                        items(
                            items = slotPending,
                            key = { "slot_${it.slot.name}_${it.medicine.id}" }
                        ) { doseItem ->
                            DoseScheduleCard(
                                item = doseItem,
                                showMemberBadge = isAllMembers,
                                onToggleTaken = { onToggleDoseTaken(doseItem) },
                                onSkip = { onMarkDoseSkipped(doseItem) },
                                onEdit = { onEditMedicine(doseItem.medicine) }
                            )
                        }
                    }
                }

                is TodayDoseFilter.Taken -> {
                    item(key = "header_only_taken_section") {
                        TakenSectionHeader(takenCount = takenDoses.size)
                    }
                    if (takenDoses.isEmpty()) {
                        item {
                            EmptyDoseFilterCard(
                                icon = Icons.Filled.CheckCircle,
                                title = "No medicines taken yet today",
                                subtitle = "Whenever you tap Take on a scheduled dose, it moves here."
                            )
                        }
                    } else {
                        items(
                            items = takenDoses,
                            key = { "only_taken_${it.slot.name}_${it.medicine.id}" }
                        ) { doseItem ->
                            DoseScheduleCard(
                                item = doseItem,
                                showMemberBadge = isAllMembers,
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
}

@Composable
private fun EmptyDoseFilterCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val displaySubName = patientName.trim().ifBlank { "Patient" }

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
                .padding(horizontal = 20.dp, vertical = 18.dp)
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displaySubName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(68.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
                            strokeWidth = 6.5.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = if (totalCount == 0) "0%" else "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$takenCount of $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.95f)
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
    selectedFilter: TodayDoseFilter,
    onSelectFilter: (TodayDoseFilter) -> Unit,
    pendingDoses: List<ScheduledDoseItem>,
    takenCount: Int
) {
    val dark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == TodayDoseFilter.All,
            onClick = { onSelectFilter(TodayDoseFilter.All) },
            label = { Text("All (${pendingDoses.size})") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("filter_all")
        )
        TimeSlot.entries.forEach { slot ->
            val count = pendingDoses.count { it.slot == slot }
            val style = rememberSlotVisualStyle(slot)
            val isSelected = selectedFilter is TodayDoseFilter.Slot && selectedFilter.slot == slot
            FilterChip(
                selected = isSelected,
                onClick = {
                    onSelectFilter(
                        if (isSelected) TodayDoseFilter.All else TodayDoseFilter.Slot(slot)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = if (isSelected) style.accent else MaterialTheme.colorScheme.onSurfaceVariant,
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
        val isTakenSelected = selectedFilter == TodayDoseFilter.Taken
        FilterChip(
            selected = isTakenSelected,
            onClick = {
                onSelectFilter(
                    if (isTakenSelected) TodayDoseFilter.All else TodayDoseFilter.Taken
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isTakenSelected) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = { Text("Taken ($takenCount)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (dark) SuccessContainerDark else SuccessContainerLight,
                selectedLabelColor = SuccessGreen
            ),
            modifier = Modifier.testTag("filter_taken")
        )
    }
}

@Composable
private fun SlotSectionHeader(
    slot: TimeSlot,
    remainingInSlot: Int
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
            Text(
                text = "${slot.displayName} • ${slot.localSubtitle}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            color = style.container,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "$remainingInSlot due",
                style = MaterialTheme.typography.labelMedium,
                color = style.accent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TakenSectionHeader(takenCount: Int) {
    val dark = isSystemInDarkTheme()
    val container = if (dark) SuccessContainerDark else SuccessContainerLight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .testTag("taken_section_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Taken",
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Taken",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            color = container,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "$takenCount taken",
                style = MaterialTheme.typography.labelMedium,
                color = SuccessGreen,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DoseScheduleCard(
    item: ScheduledDoseItem,
    showMemberBadge: Boolean,
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
                        imageVector = if (item.isTaken) Icons.Filled.CheckCircle else item.medicine.formIconCategory().iconVector(),
                        contentDescription = item.medicine.formDisplayName(),
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
                        text = "${item.medicine.dosage} • ${item.medicine.formDisplayName()}",
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

                    if (showMemberBadge) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = item.memberName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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
    showMemberBadge: Boolean,
    onMarkTaken: (ScheduledDoseItem) -> Unit,
    onEditMedicine: (Medicine) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todays_next_dose_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Today's Next Dose",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (nextDose != null) {
                        val slotStyle = rememberSlotVisualStyle(nextDose.slot)
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${nextDose.slot.displayName} • ${nextDose.formattedTime}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = slotStyle.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (nextDose != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditMedicine(nextDose.medicine) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (showMemberBadge) {
                                    "${nextDose.medicine.name} (${nextDose.memberName})"
                                } else {
                                    nextDose.medicine.name
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${nextDose.medicine.dosage} • ${nextDose.mealRelation.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onMarkTaken(nextDose) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("next_dose_take_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Take", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (allDosesCount > 0)
                                "All scheduled doses for today are complete."
                            else
                                "No active doses scheduled for today.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaysNextDoseEmptyCard(
    userProfile: com.example.model.UserProfile,
    activeMemberLabel: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todays_next_dose_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_schedule),
                contentDescription = "Empty medicine timetable illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Today's Next Dose",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
                text = if (activeMemberLabel.equals("Self", ignoreCase = true)) {
                    "Your next Morning, Afternoon, Evening, or Night medicine alert will appear here once you add your timetable below."
                } else {
                    "Your next Morning, Afternoon, Evening, or Night medicine alert for $activeMemberLabel will appear here once you add a timetable below."
                },
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
