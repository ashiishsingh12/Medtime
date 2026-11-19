package com.example.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TimeSlot(
    val displayName: String,
    val localSubtitle: String,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    MORNING("Morning", "Subah", 8, 0),
    AFTERNOON("Afternoon", "Dopahar", 13, 30),
    NIGHT("Night", "Raat", 21, 0);

    companion object {
        fun fromRaw(raw: String?): TimeSlot =
            entries.firstOrNull { it.name == raw } ?: MORNING
    }
}

enum class MedicineForm(val displayName: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    SYRUP("Syrup"),
    INJECTION("Injection"),
    DROPS("Drops"),
    INHALER("Inhaler");

    companion object {
        fun fromRaw(raw: String?): MedicineForm =
            entries.firstOrNull { it.name == raw } ?: TABLET
    }
}

enum class MealRelation(val displayName: String, val shortTag: String) {
    BEFORE_MEAL("Before Meal", "Before Meal"),
    AFTER_MEAL("After Meal", "After Meal"),
    WITH_MEAL("With Meal", "With Meal"),
    ANYTIME("Anytime", "Anytime");

    companion object {
        fun fromRaw(raw: String?): MealRelation =
            entries.firstOrNull { it.name == raw } ?: AFTER_MEAL
    }
}

enum class DoseStatus {
    TAKEN,
    SKIPPED;

    companion object {
        fun fromRaw(raw: String?): DoseStatus =
            entries.firstOrNull { it.name == raw } ?: TAKEN
    }
}

data class UserProfile(
    val displayName: String = "Patient",
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val afternoonHour: Int = 13,
    val afternoonMinute: Int = 30,
    val nightHour: Int = 21,
    val nightMinute: Int = 0
)

data class Medicine(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val form: String = MedicineForm.TABLET.name,
    val mealRelation: String = MealRelation.AFTER_MEAL.name,
    val notes: String = "",
    val morningEnabled: Boolean = true,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val afternoonEnabled: Boolean = false,
    val afternoonHour: Int = 13,
    val afternoonMinute: Int = 30,
    val nightEnabled: Boolean = false,
    val nightHour: Int = 21,
    val nightMinute: Int = 0,
    val active: Boolean = true
) {
    fun formEnum(): MedicineForm = MedicineForm.fromRaw(form)
    fun mealRelationEnum(): MealRelation = MealRelation.fromRaw(mealRelation)

    fun isSlotEnabled(slot: TimeSlot): Boolean = when (slot) {
        TimeSlot.MORNING -> morningEnabled
        TimeSlot.AFTERNOON -> afternoonEnabled
        TimeSlot.NIGHT -> nightEnabled
    }

    fun hourFor(slot: TimeSlot): Int = when (slot) {
        TimeSlot.MORNING -> morningHour
        TimeSlot.AFTERNOON -> afternoonHour
        TimeSlot.NIGHT -> nightHour
    }

    fun minuteFor(slot: TimeSlot): Int = when (slot) {
        TimeSlot.MORNING -> morningMinute
        TimeSlot.AFTERNOON -> afternoonMinute
        TimeSlot.NIGHT -> nightMinute
    }

    fun enabledSlots(): List<TimeSlot> = TimeSlot.entries.filter { isSlotEnabled(it) }
}

data class DoseLog(
    val id: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val dateKey: String = "",
    val slot: String = TimeSlot.MORNING.name,
    val status: String = DoseStatus.TAKEN.name,
    val scheduledTimeLabel: String = "",
    val takenAtEpochMillis: Long = 0L
) {
    fun slotEnum(): TimeSlot = TimeSlot.fromRaw(slot)
    fun statusEnum(): DoseStatus = DoseStatus.fromRaw(status)
}

data class MedicineInput(
    val name: String,
    val dosage: String,
    val form: MedicineForm,
    val mealRelation: MealRelation,
    val notes: String,
    val morningEnabled: Boolean,
    val morningHour: Int,
    val morningMinute: Int,
    val afternoonEnabled: Boolean,
    val afternoonHour: Int,
    val afternoonMinute: Int,
    val nightEnabled: Boolean,
    val nightHour: Int,
    val nightMinute: Int,
    val active: Boolean = true
)

data class ScheduledDoseItem(
    val medicine: Medicine,
    val slot: TimeSlot,
    val hour: Int,
    val minute: Int,
    val doseLog: DoseLog?
) {
    val isTaken: Boolean
        get() = doseLog?.statusEnum() == DoseStatus.TAKEN

    val isSkipped: Boolean
        get() = doseLog?.statusEnum() == DoseStatus.SKIPPED

    val formattedTime: String
        get() = formatTime12h(hour, minute)
}

data class DayAdherenceSummary(
    val dateKey: String,
    val dayShortLabel: String,
    val takenCount: Int,
    val scheduledCount: Int
)

fun formatTime12h(hour24: Int, minute: Int): String {
    val safeHour = hour24.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val amPm = if (safeHour < 12) "AM" else "PM"
    val hour12 = when {
        safeHour == 0 -> 12
        safeHour > 12 -> safeHour - 12
        else -> safeHour
    }
    return String.format(Locale.US, "%02d:%02d %s", hour12, safeMinute, amPm)
}

fun todayDateKey(calendar: Calendar = Calendar.getInstance()): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(calendar.time)
}

fun recentDateKeys(days: Int = 7): List<Pair<String, String>> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayFmt = SimpleDateFormat("EEE", Locale.US)
    val result = mutableListOf<Pair<String, String>>()
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
    repeat(days) {
        result.add(keyFmt.format(cal.time) to dayFmt.format(cal.time))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}

fun formatDisplayDate(dateKey: String): String {
    val today = todayDateKey()
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = todayDateKey(cal)
    return when (dateKey) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val display = SimpleDateFormat("EEE, MMM d", Locale.US)
            val parsed = parser.parse(dateKey)
            if (parsed != null) display.format(parsed) else dateKey
        } catch (_: Exception) {
            dateKey
        }
    }
}

fun buildDoseLogId(dateKey: String, slot: TimeSlot, medicineId: String): String {
    val sanitizedMedId = medicineId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
    return "${dateKey}_${slot.name}_${sanitizedMedId}".take(128)
}
