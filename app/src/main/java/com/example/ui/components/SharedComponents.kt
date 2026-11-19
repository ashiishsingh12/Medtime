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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.model.MedicineForm
import com.example.model.TimeSlot
import com.example.ui.theme.AfternoonAccent
import com.example.ui.theme.AfternoonSurfaceDark
import com.example.ui.theme.AfternoonSurfaceLight
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
                text = "${slot.displayName} • $timeLabel",
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
