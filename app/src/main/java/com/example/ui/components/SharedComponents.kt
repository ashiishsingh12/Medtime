package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MedicineForm
import com.example.model.RecurrenceEndType
import com.example.model.RecurrenceFrequency
import com.example.model.RecurrenceRule
import com.example.model.TimeSlot
import com.example.model.WEEKDAY_OPTIONS
import com.example.model.computeEffectiveEndDateKey
import com.example.model.dateKeyToUtcMillis
import com.example.model.dayOfWeekForDateKey
import com.example.model.formatDateKeyDmy
import com.example.model.formatRecurrenceSummary
import com.example.model.shiftDateKey
import com.example.model.todayDateKey
import com.example.model.todayUtcMillis
import com.example.model.utcMillisToDateKey
import com.example.model.weekdayFullName
import com.example.ui.theme.AfternoonAccent
import com.example.ui.theme.AfternoonSurfaceDark
import com.example.ui.theme.AfternoonSurfaceLight
import com.example.ui.theme.EveningAccent
import com.example.ui.theme.EveningSurfaceDark
import com.example.ui.theme.EveningSurfaceLight
import com.example.ui.theme.MorningAccent
import com.example.ui.theme.MorningSurfaceDark
import com.example.ui.theme.MorningSurfaceLight
import com.example.ui.theme.NightAccent
import com.example.ui.theme.NightSurfaceDark
import com.example.ui.theme.NightSurfaceLight

data class SlotVisualStyle(
    val accent: Color,
    val container: Color,
    val icon: ImageVector
)

@Composable
fun rememberSlotVisualStyle(slot: TimeSlot): SlotVisualStyle {
    val dark = isSystemInDarkTheme()
    return when (slot) {
        TimeSlot.MORNING -> SlotVisualStyle(
            accent = MorningAccent,
            container = if (dark) MorningSurfaceDark else MorningSurfaceLight,
            icon = Icons.Filled.WbTwilight
        )
        TimeSlot.AFTERNOON -> SlotVisualStyle(
            accent = AfternoonAccent,
            container = if (dark) AfternoonSurfaceDark else AfternoonSurfaceLight,
            icon = Icons.Filled.WbSunny
        )
        TimeSlot.EVENING -> SlotVisualStyle(
            accent = EveningAccent,
            container = if (dark) EveningSurfaceDark else EveningSurfaceLight,
            icon = Icons.Filled.NightsStay
        )
        TimeSlot.NIGHT -> SlotVisualStyle(
            accent = NightAccent,
            container = if (dark) NightSurfaceDark else NightSurfaceLight,
            icon = Icons.Filled.Bedtime
        )
    }
}

fun MedicineForm.iconVector(): ImageVector = when (this) {
    MedicineForm.TABLET -> Icons.Filled.Medication
    MedicineForm.CAPSULE -> Icons.Filled.Medication
    MedicineForm.SYRUP -> Icons.Filled.LocalDrink
    MedicineForm.INJECTION -> Icons.Filled.Vaccines
    MedicineForm.DROPS -> Icons.Filled.WaterDrop
    MedicineForm.INHALER -> Icons.Filled.Air
}

@Composable
fun SlotChipBadge(
    slot: TimeSlot,
    timeLabel: String,
    mealLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val style = rememberSlotVisualStyle(slot)
    Surface(
        color = style.container,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (mealLabel.isNullOrBlank()) "${slot.displayName} • $timeLabel"
                else "${slot.displayName} • $timeLabel • $mealLabel",
                style = MaterialTheme.typography.labelSmall,
                color = style.accent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedSingleDatePickerDialog(
    title: String = "Select Date",
    initialDateKey: String?,
    onDismiss: () -> Unit,
    onConfirm: (dateKey: String) -> Unit
) {
    val initialMillis = initialDateKey
        ?.takeIf { it.isNotBlank() }
        ?.let(::dateKeyToUtcMillis)
        ?: todayUtcMillis()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    val dmyDateFormatter = remember {
        DatePickerDefaults.dateFormatter(
            selectedDateSkeleton = "dd-MM-yyyy"
        )
    }
    val selectedDateKey = datePickerState.selectedDateMillis?.let(::utcMillisToDateKey)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedDateKey != null) {
                        onConfirm(selectedDateKey)
                    }
                },
                enabled = selectedDateKey != null
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            dateFormatter = dmyDateFormatter,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            },
            headline = {
                Text(
                    text = selectedDateKey?.let(::formatDateKeyDmy) ?: "dd-mm-yyyy",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                )
            },
            showModeToggle = false
        )
    }
}

private enum class RecurrenceDatePickerTarget {
    START_DATE,
    END_DATE
}

@Composable
fun GoogleCalendarRecurrenceDialog(
    title: String = "Custom recurrence",
    medicineName: String? = null,
    initialRule: RecurrenceRule,
    isSaving: Boolean = false,
    confirmButtonLabel: String = "Done",
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule) -> Unit
) {
    val today = remember { todayDateKey() }
    val initialStart = remember(initialRule) {
        initialRule.startDateKey?.takeIf { it.isNotBlank() } ?: today
    }
    var startDateKey by remember(initialRule) { mutableStateOf(initialStart) }
    var frequency by remember(initialRule) { mutableStateOf(initialRule.frequency) }
    var intervalText by remember(initialRule) {
        mutableStateOf(initialRule.interval.coerceIn(1, 99).toString())
    }
    var selectedDays by remember(initialRule) {
        mutableStateOf(
            initialRule.daysOfWeek.ifEmpty { setOf(dayOfWeekForDateKey(initialStart)) }
        )
    }
    var endType by remember(initialRule) { mutableStateOf(initialRule.endType) }
    var endDateKey by remember(initialRule) {
        val currentEnd = initialRule.endDateKey?.takeIf { it.isNotBlank() }
        val fallbackEnd = shiftDateKey(maxOf(initialStart, today), 6)
        mutableStateOf(
            when {
                currentEnd != null && currentEnd >= today -> currentEnd
                else -> fallbackEnd
            }
        )
    }
    var occurrencesText by remember(initialRule) {
        mutableStateOf((initialRule.occurrenceCount ?: 7).coerceIn(1, 365).toString())
    }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<RecurrenceDatePickerTarget?>(null) }

    when (datePickerTarget) {
        RecurrenceDatePickerTarget.START_DATE -> {
            MedSingleDatePickerDialog(
                title = "Starts on (dd-mm-yyyy)",
                initialDateKey = startDateKey,
                onDismiss = { datePickerTarget = null },
                onConfirm = { picked ->
                    startDateKey = picked
                    if (endDateKey < picked) {
                        endDateKey = picked
                    }
                    datePickerTarget = null
                }
            )
        }
        RecurrenceDatePickerTarget.END_DATE -> {
            MedSingleDatePickerDialog(
                title = "Ends on (dd-mm-yyyy)",
                initialDateKey = endDateKey,
                onDismiss = { datePickerTarget = null },
                onConfirm = { picked ->
                    endDateKey = if (picked < startDateKey) startDateKey else picked
                    endType = RecurrenceEndType.ON_DATE
                    datePickerTarget = null
                }
            )
        }
        null -> Unit
    }

    val safeInterval = intervalText.toIntOrNull()?.coerceIn(1, 99) ?: 1
    val safeOccurrences = occurrencesText.toIntOrNull()?.coerceIn(1, 365) ?: 1
    val previewRule = RecurrenceRule(
        frequency = frequency,
        interval = safeInterval,
        daysOfWeek = if (frequency == RecurrenceFrequency.WEEKLY) selectedDays else emptySet(),
        endType = if (frequency == RecurrenceFrequency.NONE) RecurrenceEndType.ON_DATE else endType,
        startDateKey = startDateKey,
        endDateKey = when {
            frequency == RecurrenceFrequency.NONE -> startDateKey
            endType == RecurrenceEndType.ON_DATE -> endDateKey
            else -> null
        },
        occurrenceCount = if (endType == RecurrenceEndType.AFTER_OCCURRENCES) safeOccurrences else null
    )
    val effectiveEnd = computeEffectiveEndDateKey(previewRule)
    val invalidEndBeforeStart = frequency != RecurrenceFrequency.NONE &&
        endType == RecurrenceEndType.ON_DATE &&
        endDateKey < startDateKey

    val displayName = medicineName?.trim()?.takeIf { it.isNotBlank() }
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EventRepeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayName != null) {
                    val prevEnded = initialRule.endDateKey?.takeIf { it.isNotBlank() && it < today }
                    Text(
                        text = if (prevEnded != null) {
                            "$displayName ended on ${formatDateKeyDmy(prevEnded)}. Update its repeat rule below."
                        } else {
                            "Set recurring schedule for $displayName."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Starts on row (dd-mm-yyyy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Starts on",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { datePickerTarget = RecurrenceDatePickerTarget.START_DATE }
                            .testTag("recurrence_start_date_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Pick start date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatDateKeyDmy(startDateKey),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Repeat every [N] [unit]
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Repeat every",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (frequency != RecurrenceFrequency.NONE) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val next = (safeInterval - 1).coerceAtLeast(1)
                                            intervalText = next.toString()
                                        }
                                        .testTag("recurrence_interval_minus")
                                ) {
                                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Remove,
                                            contentDescription = "Decrease interval",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = intervalText,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(2)
                                        intervalText = digits
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.titleSmall.copy(textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .width(56.dp)
                                        .testTag("recurrence_interval_input")
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val next = (safeInterval + 1).coerceAtMost(99)
                                            intervalText = next.toString()
                                        }
                                        .testTag("recurrence_interval_plus")
                                ) {
                                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Increase interval",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { unitMenuExpanded = true }
                                    .testTag("recurrence_unit_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (frequency == RecurrenceFrequency.NONE) {
                                            RecurrenceFrequency.NONE.label
                                        } else {
                                            frequency.unitLabel(safeInterval)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Choose repeat unit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = unitMenuExpanded,
                                onDismissRequest = { unitMenuExpanded = false }
                            ) {
                                listOf(
                                    RecurrenceFrequency.DAILY,
                                    RecurrenceFrequency.WEEKLY,
                                    RecurrenceFrequency.MONTHLY,
                                    RecurrenceFrequency.YEARLY,
                                    RecurrenceFrequency.NONE
                                ).forEach { option ->
                                    val label = if (option == RecurrenceFrequency.NONE) {
                                        option.label
                                    } else {
                                        option.unitLabel(safeInterval)
                                    }
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        trailingIcon = if (frequency == option) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            frequency = option
                                            if (option == RecurrenceFrequency.WEEKLY && selectedDays.isEmpty()) {
                                                selectedDays = setOf(dayOfWeekForDateKey(startDateKey))
                                            }
                                            unitMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Repeat on S M T W T F S (when weekly)
                if (frequency == RecurrenceFrequency.WEEKLY) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Repeat on",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WEEKDAY_OPTIONS.forEach { (dayInt, shortLetter) ->
                                val isSelected = dayInt in selectedDays
                                val fullDayName = weekdayFullName(dayInt)
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            val updated = if (isSelected) {
                                                if (selectedDays.size > 1) selectedDays - dayInt else selectedDays
                                            } else {
                                                selectedDays + dayInt
                                            }
                                            selectedDays = updated
                                        }
                                        .semantics { contentDescription = fullDayName }
                                        .testTag("recurrence_day_$dayInt"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortLetter,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Ends (Never / On <date> / After <N> occurrences)
                if (frequency != RecurrenceFrequency.NONE) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Ends",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Never
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { endType = RecurrenceEndType.NEVER }
                                .padding(vertical = 2.dp)
                                .testTag("recurrence_end_never"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = endType == RecurrenceEndType.NEVER,
                                onClick = { endType = RecurrenceEndType.NEVER }
                            )
                            Text(
                                text = "Never",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // On <date>
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { endType = RecurrenceEndType.ON_DATE }
                                .padding(vertical = 2.dp)
                                .testTag("recurrence_end_on"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = endType == RecurrenceEndType.ON_DATE,
                                onClick = { endType = RecurrenceEndType.ON_DATE }
                            )
                            Text(
                                text = "On",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(34.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (endType == RecurrenceEndType.ON_DATE) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (endType == RecurrenceEndType.ON_DATE) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    }
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        endType = RecurrenceEndType.ON_DATE
                                        datePickerTarget = RecurrenceDatePickerTarget.END_DATE
                                    }
                                    .testTag("recurrence_end_date_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = "Pick end date",
                                        tint = if (endType == RecurrenceEndType.ON_DATE) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = formatDateKeyDmy(endDateKey),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (endType == RecurrenceEndType.ON_DATE) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        // After <N> occurrences
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { endType = RecurrenceEndType.AFTER_OCCURRENCES }
                                .padding(vertical = 2.dp)
                                .testTag("recurrence_end_after"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = endType == RecurrenceEndType.AFTER_OCCURRENCES,
                                onClick = { endType = RecurrenceEndType.AFTER_OCCURRENCES }
                            )
                            Text(
                                text = "After",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = occurrencesText,
                                onValueChange = { raw ->
                                    endType = RecurrenceEndType.AFTER_OCCURRENCES
                                    occurrencesText = raw.filter { it.isDigit() }.take(3)
                                },
                                singleLine = true,
                                enabled = endType == RecurrenceEndType.AFTER_OCCURRENCES,
                                textStyle = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .width(58.dp)
                                    .testTag("recurrence_occurrences_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (safeOccurrences == 1) "occurrence" else "occurrences",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Live summary preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (invalidEndBeforeStart) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (invalidEndBeforeStart) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (invalidEndBeforeStart) {
                            "End date cannot be earlier than start date (${formatDateKeyDmy(startDateKey)})."
                        } else {
                            val summary = formatRecurrenceSummary(previewRule)
                            if (effectiveEnd != null && endType == RecurrenceEndType.NEVER) {
                                summary
                            } else {
                                summary
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (invalidEndBeforeStart) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(previewRule) },
                enabled = !isSaving && !invalidEndBeforeStart,
                modifier = Modifier.testTag("confirm_recurrence_button")
            ) {
                Text(if (isSaving) "Saving..." else confirmButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

