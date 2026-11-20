package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Medicine
import com.example.model.TimeSlot
import com.example.model.formatTime12h
import com.example.ui.components.SlotChipBadge
import com.example.ui.components.iconVector

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicinesScreen(
    medicines: List<Medicine>,
    onAddMedicine: () -> Unit,
    onEditMedicine: (Medicine) -> Unit,
    onToggleActive: (Medicine, Boolean) -> Unit,
    onDeleteMedicine: (Medicine) -> Unit,
    onTestNotification: (Medicine) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<Medicine?>(null) }

    pendingDelete?.let { medToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${medToDelete.name}?") },
            text = {
                Text("This will stop its morning, afternoon, evening, and night reminder alerts and remove it from your timetable.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMedicine(medToDelete)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_medicine_button")
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Medicine Timetable",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${medicines.count { it.active }} active • ${medicines.size} total medicines",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddMedicine,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("timetable_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }

        if (medicines.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No medicines added yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap Add above to create a morning, afternoon, evening, or night schedule with alert notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = medicines, key = { it.id }) { medicine ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (medicine.active) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_card_${medicine.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = medicine.formEnum().iconVector(),
                                    contentDescription = medicine.formEnum().displayName,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = medicine.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${medicine.dosage} • ${medicine.formEnum().displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = medicine.active,
                                onCheckedChange = { onToggleActive(medicine, it) },
                                modifier = Modifier.testTag("medicine_active_switch_${medicine.id}")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeSlot.entries.forEach { slot ->
                                if (medicine.isSlotEnabled(slot)) {
                                    SlotChipBadge(
                                        slot = slot,
                                        timeLabel = formatTime12h(
                                            medicine.hourFor(slot),
                                            medicine.minuteFor(slot)
                                        ),
                                        mealLabel = medicine.mealRelationFor(slot).shortTag
                                    )
                                }
                            }
                        }

                        if (medicine.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = medicine.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onTestNotification(medicine) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("test_alert_button_${medicine.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Alert", style = MaterialTheme.typography.labelMedium)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditMedicine(medicine) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("edit_medicine_button_${medicine.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit ${medicine.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { pendingDelete = medicine },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_medicine_button_${medicine.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete ${medicine.name}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
