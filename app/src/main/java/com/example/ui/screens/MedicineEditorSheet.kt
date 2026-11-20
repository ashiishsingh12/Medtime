package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val initialForm = existingMedicine?.formEnum() ?: MedicineForm.TABLET
    val initialDosage = existingMedicine?.dosage ?: ""
    val initialIsCustom = existingMedicine != null &&
        initialDosage.isNotBlank() &&
        initialDosage !in initialForm.dosagePresets()

    var form by remember(existingMedicine) { mutableStateOf(initialForm) }
    var name by remember(existingMedicine) { mutableStateOf(existingMedicine?.name ?: "") }
    var dosage by remember(existingMedicine) { mutableStateOf(initialDosage) }
    var isCustomDosage by remember(existingMedicine) { mutableStateOf(initialIsCustom) }
    var notes by remember(existingMedicine) { mutableStateOf(existingMedicine?.notes ?: "") }

    var formMenuExpanded by remember { mutableStateOf(false) }
    var dosageMenuExpanded by remember { mutableStateOf(false) }

    var morningEnabled by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.morningEnabled ?: true)
    }
    var morningHour by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.morningHour ?: userProfile.morningHour)
    }
    var morningMinute by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.morningMinute ?: userProfile.morningMinute)
    }
    var morningMealRelation by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.mealRelationFor(TimeSlot.MORNING) ?: MealRelation.AFTER_MEAL)
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
    var afternoonMealRelation by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.mealRelationFor(TimeSlot.AFTERNOON) ?: MealRelation.AFTER_MEAL)
    }

    var eveningEnabled by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.eveningEnabled ?: false)
    }
    var eveningHour by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.eveningHour ?: userProfile.eveningHour)
    }
    var eveningMinute by remember(existingMedicine, userProfile) {
        mutableIntStateOf(existingMedicine?.eveningMinute ?: userProfile.eveningMinute)
    }
    var eveningMealRelation by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.mealRelationFor(TimeSlot.EVENING) ?: MealRelation.AFTER_MEAL)
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
    var nightMealRelation by remember(existingMedicine) {
        mutableStateOf(existingMedicine?.mealRelationFor(TimeSlot.NIGHT) ?: MealRelation.AFTER_MEAL)
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var activeTimePickerSlot by remember { mutableStateOf<TimeSlot?>(null) }

    val dependentDosageOptions = remember(form) { form.dosagePresets() }

    val activeSlotLabels = buildList {
        if (morningEnabled) add("Morning ${formatTime12h(morningHour, morningMinute)}")
        if (afternoonEnabled) add("Afternoon ${formatTime12h(afternoonHour, afternoonMinute)}")
        if (eveningEnabled) add("Evening ${formatTime12h(eveningHour, eveningMinute)}")
        if (nightEnabled) add("Night ${formatTime12h(nightHour, nightMinute)}")
    }

    activeTimePickerSlot?.let { slot ->
        val (initH, initM) = when (slot) {
            TimeSlot.MORNING -> morningHour to morningMinute
            TimeSlot.AFTERNOON -> afternoonHour to afternoonMinute
            TimeSlot.EVENING -> eveningHour to eveningMinute
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
                    TimeSlot.EVENING -> {
                        eveningHour = h
                        eveningMinute = m
                        eveningEnabled = true
                    }
                    TimeSlot.NIGHT -> {
                        nightHour = h
                        nightMinute = m
                        nightEnabled = true
                    }
                }
                validationError = null
                activeTimePickerSlot = null
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Pinned Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (existingMedicine == null) stringResource(R.string.add_medicine)
                        else stringResource(R.string.edit_medicine),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose medicine type, name, dosage & per-slot meal timing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Scrollable Form Body
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // 1. FIRST: Medicine Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = formMenuExpanded,
                    onExpandedChange = { formMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = form.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Medicine Type") },
                        leadingIcon = {
                            Icon(
                                imageVector = form.iconVector(),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = formMenuExpanded)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                            .testTag("medicine_type_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = formMenuExpanded,
                        onDismissRequest = { formMenuExpanded = false }
                    ) {
                        MedicineForm.entries.forEach { itemForm ->
                            DropdownMenuItem(
                                text = { Text(itemForm.displayName) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = itemForm.iconVector(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = if (form == itemForm) {
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
                                    if (form != itemForm) {
                                        form = itemForm
                                        if (dosage !in itemForm.dosagePresets()) {
                                            dosage = ""
                                            isCustomDosage = false
                                        }
                                        validationError = null
                                    }
                                    formMenuExpanded = false
                                },
                                modifier = Modifier.testTag("form_option_${itemForm.name.lowercase()}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. SECOND: Medicine Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 80) name = it
                        validationError = null
                    },
                    label = { Text("Medicine Name") },
                    placeholder = { Text("e.g., Metformin 500mg, Paracetamol") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. THIRD: Dependent Dosage Dropdown (with Custom manual input option)
                if (!isCustomDosage) {
                    ExposedDropdownMenuBox(
                        expanded = dosageMenuExpanded,
                        onExpandedChange = { dosageMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dosage (${form.displayName})") },
                            placeholder = { Text("Select ${form.displayName.lowercase()} dosage") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageMenuExpanded)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                                .testTag("medicine_dosage_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = dosageMenuExpanded,
                            onDismissRequest = { dosageMenuExpanded = false }
                        ) {
                            dependentDosageOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    trailingIcon = if (dosage == option) {
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
                                        dosage = option
                                        isCustomDosage = false
                                        validationError = null
                                        dosageMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            DropdownMenuItem(
                                text = { Text("Custom (Type manually)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    dosage = ""
                                    isCustomDosage = true
                                    validationError = null
                                    dosageMenuExpanded = false
                                },
                                modifier = Modifier.testTag("dosage_custom_option")
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = {
                            if (it.length <= 40) dosage = it
                            validationError = null
                        },
                        label = { Text("Custom Dosage (${form.displayName})") },
                        placeholder = {
                            Text(
                                when (form) {
                                    MedicineForm.TABLET, MedicineForm.CAPSULE -> "e.g., 1.5 tablets"
                                    MedicineForm.SYRUP -> "e.g., 7.5 ml"
                                    MedicineForm.INJECTION -> "e.g., 15 units, 1.5 ml"
                                    MedicineForm.DROPS -> "e.g., 5 drops"
                                    MedicineForm.INHALER -> "e.g., 3 puffs"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        trailingIcon = {
                            Box {
                                IconButton(
                                    onClick = { dosageMenuExpanded = true },
                                    modifier = Modifier.testTag("dosage_presets_switcher")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Choose preset dosage"
                                    )
                                }
                                DropdownMenu(
                                    expanded = dosageMenuExpanded,
                                    onDismissRequest = { dosageMenuExpanded = false }
                                ) {
                                    dependentDosageOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                dosage = option
                                                isCustomDosage = false
                                                validationError = null
                                                dosageMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_dosage_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. FOURTH: Time Table & Alert Times (with individual Meal Timing per slot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Time Table, Alerts & Meal Timing",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${activeSlotLabels.size} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SlotToggleRow(
                        slot = TimeSlot.MORNING,
                        enabled = morningEnabled,
                        hour = morningHour,
                        minute = morningMinute,
                        mealRelation = morningMealRelation,
                        onToggle = {
                            morningEnabled = it
                            validationError = null
                        },
                        onPickTime = { activeTimePickerSlot = TimeSlot.MORNING },
                        onSelectMeal = { morningMealRelation = it }
                    )
                    SlotToggleRow(
                        slot = TimeSlot.AFTERNOON,
                        enabled = afternoonEnabled,
                        hour = afternoonHour,
                        minute = afternoonMinute,
                        mealRelation = afternoonMealRelation,
                        onToggle = {
                            afternoonEnabled = it
                            validationError = null
                        },
                        onPickTime = { activeTimePickerSlot = TimeSlot.AFTERNOON },
                        onSelectMeal = { afternoonMealRelation = it }
                    )
                    SlotToggleRow(
                        slot = TimeSlot.EVENING,
                        enabled = eveningEnabled,
                        hour = eveningHour,
                        minute = eveningMinute,
                        mealRelation = eveningMealRelation,
                        onToggle = {
                            eveningEnabled = it
                            validationError = null
                        },
                        onPickTime = { activeTimePickerSlot = TimeSlot.EVENING },
                        onSelectMeal = { eveningMealRelation = it }
                    )
                    SlotToggleRow(
                        slot = TimeSlot.NIGHT,
                        enabled = nightEnabled,
                        hour = nightHour,
                        minute = nightMinute,
                        mealRelation = nightMealRelation,
                        onToggle = {
                            nightEnabled = it
                            validationError = null
                        },
                        onPickTime = { activeTimePickerSlot = TimeSlot.NIGHT },
                        onSelectMeal = { nightMealRelation = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Optional Instructions / Notes
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

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Pinned Sticky Save Footer
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    if (validationError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = validationError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        Text(
                            text = if (activeSlotLabels.isEmpty()) {
                                "Turn on at least one alert slot above"
                            } else {
                                "Daily alerts: ${activeSlotLabels.joinToString(" • ")}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedDosage = dosage.trim()
                            when {
                                trimmedName.isEmpty() -> {
                                    validationError = "Please enter the medicine name."
                                }
                                trimmedDosage.isEmpty() -> {
                                    validationError = "Please select or enter the dosage."
                                }
                                !morningEnabled && !afternoonEnabled && !eveningEnabled && !nightEnabled -> {
                                    validationError = "Please turn on at least one slot (Morning, Afternoon, Evening, or Night)."
                                }
                                else -> {
                                    val primaryMeal = when {
                                        morningEnabled -> morningMealRelation
                                        afternoonEnabled -> afternoonMealRelation
                                        eveningEnabled -> eveningMealRelation
                                        else -> nightMealRelation
                                    }
                                    onSave(
                                        existingMedicine?.id,
                                        MedicineInput(
                                            name = trimmedName,
                                            dosage = trimmedDosage,
                                            form = form,
                                            mealRelation = primaryMeal,
                                            notes = notes.trim(),
                                            morningEnabled = morningEnabled,
                                            morningHour = morningHour,
                                            morningMinute = morningMinute,
                                            morningMealRelation = morningMealRelation,
                                            afternoonEnabled = afternoonEnabled,
                                            afternoonHour = afternoonHour,
                                            afternoonMinute = afternoonMinute,
                                            afternoonMealRelation = afternoonMealRelation,
                                            eveningEnabled = eveningEnabled,
                                            eveningHour = eveningHour,
                                            eveningMinute = eveningMinute,
                                            eveningMealRelation = eveningMealRelation,
                                            nightEnabled = nightEnabled,
                                            nightHour = nightHour,
                                            nightMinute = nightMinute,
                                            nightMealRelation = nightMealRelation,
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
                            .height(52.dp)
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
                            Text("Saving & Scheduling Alerts...")
                        } else {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (existingMedicine == null) "Save & Set Reminders"
                                else "Update & Save Reminders",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotToggleRow(
    slot: TimeSlot,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    mealRelation: MealRelation,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    onSelectMeal: (MealRelation) -> Unit
) {
    val style = rememberSlotVisualStyle(slot)
    var mealMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) style.container.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) style.accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggle(!enabled) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = slot.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (enabled) "${slot.localSubtitle} • Alert on" else "${slot.localSubtitle} • Off",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enabled) style.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val timeTint = if (enabled) style.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                val timeBorder = if (enabled) style.accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

                OutlinedButton(
                    onClick = onPickTime,
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, timeBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = style.accent,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .testTag("pick_time_${slot.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = timeTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTime12h(hour, minute),
                        style = MaterialTheme.typography.labelSmall,
                        color = timeTint,
                        maxLines = 1
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("slot_switch_${slot.name.lowercase()}")
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = style.accent.copy(alpha = 0.18f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, style.accent.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = enabled) { mealMenuExpanded = true }
                                .testTag("slot_meal_dropdown_${slot.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restaurant,
                                    contentDescription = null,
                                    tint = style.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Meal Timing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = mealRelation.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = style.accent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (mealMenuExpanded) Icons.Filled.KeyboardArrowUp
                                    else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Select ${slot.displayName} meal timing",
                                    tint = style.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = mealMenuExpanded,
                            onDismissRequest = { mealMenuExpanded = false }
                        ) {
                            MealRelation.entries.forEach { relation ->
                                DropdownMenuItem(
                                    text = { Text(relation.displayName) },
                                    trailingIcon = if (mealRelation == relation) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = style.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        onSelectMeal(relation)
                                        mealMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag(
                                        "slot_${slot.name.lowercase()}_meal_${relation.name.lowercase()}"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
