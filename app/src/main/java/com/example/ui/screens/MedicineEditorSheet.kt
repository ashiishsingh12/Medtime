package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MealRelation
import com.example.model.Medicine
import com.example.model.MedicineForm
import com.example.model.MedicineInput
import com.example.model.TimeSlot
import com.example.model.UserProfile
import com.example.model.formatTime12h
import com.example.ui.components.MedTimePickerDialog
import com.example.ui.components.iconVector
import com.example.ui.components.rememberSlotVisualStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicineEditorSheet(
    existingMedicine: Medicine?,
    userProfile: UserProfile,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (medicineId: String?, input: MedicineInput) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(existingMedicine) { mutableStateOf(existingMedicine?.name ?: "") }
    var dosage by remember(existingMedicine) { mutableStateOf(existingMedicine?.dosage ?: "") }
    var form by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.formEnum() ?: MedicineForm.TABLET)
    }
    var mealRelation by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.mealRelationEnum() ?: MealRelation.AFTER_MEAL)
    }
    var notes by remember(existingMedicine) { mutableStateOf(existingMedicine?.notes ?: "") }

    var morningEnabled by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.morningEnabled ?: true)
    }
    var morningHour by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.morningHour ?: userProfile.morningHour)
    }
    var morningMinute by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.morningMinute ?: userProfile.morningMinute)
    }

    var afternoonEnabled by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.afternoonEnabled ?: false)
    }
    var afternoonHour by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.afternoonHour ?: userProfile.afternoonHour)
    }
    var afternoonMinute by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.afternoonMinute ?: userProfile.afternoonMinute)
    }

    var nightEnabled by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.nightEnabled ?: true)
    }
    var nightHour by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.nightHour ?: userProfile.nightHour)
    }
    var nightMinute by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.nightMinute ?: userProfile.nightMinute)
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var activeTimePickerSlot by remember { mutableStateOf<TimeSlot?>(null) }

    activeTimePickerSlot?.let { slot ->
        val (initH, initM) = when (slot) {
            TimeSlot.MORNING -> morningHour to morningMinute
            TimeSlot.AFTERNOON -> afternoonHour to afternoonMinute
            TimeSlot.NIGHT -> nightHour to nightMinute
        }
        MedTimePickerDialog(
            title = "${slot.displayName} Reminder Time",
            initialHour = initH,
            initialMinute = initM,
            onDismiss = { activeTimePickerSlot = null },
            onConfirm = { h, m ->
                when (slot) {
                    TimeSlot.MORNING -> {
                        morningHour = h
                        morningMinute = m
                        morningEnabled = true
                    }
                    TimeSlot.AFTERNOON -> {
                        afternoonHour = h
                        afternoonMinute = m
                        afternoonEnabled = true
                    }
                    TimeSlot.NIGHT -> {
                        nightHour = h
                        nightMinute = m
                        nightEnabled = true
                    }
                }
                activeTimePickerSlot = null
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingMedicine == null) stringResource(R.string.add_medicine)
                    else stringResource(R.string.edit_medicine),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close editor"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 80) name = it
                    validationError = null
                },
                label = { Text("Medicine Name") },
                placeholder = { Text("e.g., Metformin 500mg, Paracetamol") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dosage,
                onValueChange = {
                    if (it.length <= 40) dosage = it
                    validationError = null
                },
                label = { Text("Dosage") },
                placeholder = { Text("e.g., 1 tablet, 5 ml") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_dosage_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("1 tablet", "1/2 tablet", "1 capsule", "5 ml", "10 ml", "2 drops").forEach { preset ->
                    SuggestionChip(
                        onClick = {
                            dosage = preset
                            validationError = null
                        },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Medicine Type",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MedicineForm.entries.forEach { itemForm ->
                    FilterChip(
                        selected = form == itemForm,
                        onClick = { form = itemForm },
                        leadingIcon = {
                            Icon(
                                imageVector = itemForm.iconVector(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(itemForm.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Time Table & Alert Times",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SlotToggleRow(
                    slot = TimeSlot.MORNING,
                    enabled = morningEnabled,
                    hour = morningHour,
                    minute = morningMinute,
                    onToggle = {
                        morningEnabled = it
                        validationError = null
                    },
                    onPickTime = { activeTimePickerSlot = TimeSlot.MORNING }
                )
                SlotToggleRow(
                    slot = TimeSlot.AFTERNOON,
                    enabled = afternoonEnabled,
                    hour = afternoonHour,
                    minute = afternoonMinute,
                    onToggle = {
                        afternoonEnabled = it
                        validationError = null
                    },
                    onPickTime = { activeTimePickerSlot = TimeSlot.AFTERNOON }
                )
                SlotToggleRow(
                    slot = TimeSlot.NIGHT,
                    enabled = nightEnabled,
                    hour = nightHour,
                    minute = nightMinute,
                    onToggle = {
                        nightEnabled = it
                        validationError = null
                    },
                    onPickTime = { activeTimePickerSlot = TimeSlot.NIGHT }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Meal Timing",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealRelation.entries.forEach { relation ->
                    FilterChip(
                        selected = mealRelation == relation,
                        onClick = { mealRelation = relation },
                        label = { Text(relation.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 200) notes = it },
                label = { Text("Instructions / Notes (Optional)") },
                placeholder = { Text("e.g., Drink full glass of warm water") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_notes_input")
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedDosage = dosage.trim()
                    when {
                        trimmedName.isEmpty() -> {
                            validationError = "Please enter the medicine name."
                        }
                        trimmedDosage.isEmpty() -> {
                            validationError = "Please enter the dosage (for example, 1 tablet)."
                        }
                        !morningEnabled && !afternoonEnabled && !nightEnabled -> {
                            validationError = "Please turn on at least one slot (Morning, Afternoon, or Night)."
                        }
                        else -> {
                            onSave(
                                existingMedicine?.id,
                                MedicineInput(
                                    name = trimmedName,
                                    dosage = trimmedDosage,
                                    form = form,
                                    mealRelation = mealRelation,
                                    notes = notes.trim(),
                                    morningEnabled = morningEnabled,
                                    morningHour = morningHour,
                                    morningMinute = morningMinute,
                                    afternoonEnabled = afternoonEnabled,
                                    afternoonHour = afternoonHour,
                                    afternoonMinute = afternoonMinute,
                                    nightEnabled = nightEnabled,
                                    nightHour = nightHour,
                                    nightMinute = nightMinute,
                                    active = existingMedicine?.active ?: true
                                )
                            )
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("save_medicine_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving Timetable...")
                } else {
                    Text(
                        text = if (existingMedicine == null) "Save to Timetable" else "Update Medicine",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SlotToggleRow(
    slot: TimeSlot,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit
) {
    val style = rememberSlotVisualStyle(slot)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) style.container.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) style.accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(style.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = slot.displayName,
                    tint = style.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${slot.displayName} (${slot.localSubtitle})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (enabled) "Alert at ${formatTime12h(hour, minute)}" else "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) style.accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onPickTime,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, style.accent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("pick_time_${slot.name.lowercase()}")
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = style.accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime12h(hour, minute),
                    style = MaterialTheme.typography.labelSmall,
                    color = style.accent
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("slot_switch_${slot.name.lowercase()}")
            )
        }
    }
}
