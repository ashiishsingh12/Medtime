package com.example.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class TimeSlot(
    val displayName: String,
    val localSubtitle: String,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    MORNING("Morning", "Subah", 8, 0),
    AFTERNOON("Afternoon", "Dopahar", 13, 30),
    EVENING("Evening", "Shaam", 18, 0),
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

    fun dosagePresets(): List<String> = when (this) {
        TABLET -> listOf("1/4 tablet", "1/2 tablet", "1 tablet", "1.5 tablets", "2 tablets")
        CAPSULE -> listOf("1 capsule", "2 capsules", "1/2 capsule")
        SYRUP -> listOf("1 ml", "2 ml", "2.5 ml", "5 ml (1 tsp)", "10 ml (2 tsp)", "15 ml")
        INJECTION -> listOf(
            "10 units (Insulin)",
            "20 units (Insulin)",
            "0.5 ml (Vaccine / IM)",
            "1 ml (IM / IV)",
            "2 ml (IM / IV)",
            "1 vial",
            "1 ampoule"
        )
        DROPS -> listOf("1 drop", "2 drops", "3 drops", "4 drops")
        INHALER -> listOf("1 puff", "2 puffs", "1 spray", "2 sprays")
    }

    companion object {
        fun fromRaw(raw: String?): MedicineForm =
            fromRawOrNull(raw) ?: TABLET

        fun fromRawOrNull(raw: String?): MedicineForm? {
            val cleaned = raw?.trim().orEmpty()
            if (cleaned.isEmpty()) return null
            return entries.firstOrNull {
                it.name.equals(cleaned, ignoreCase = true) ||
                    it.displayName.equals(cleaned, ignoreCase = true)
            }
        }

        fun inferIconCategory(raw: String?): MedicineForm {
            val lower = raw?.trim()?.lowercase().orEmpty()
            return when {
                lower.contains("inhaler") || lower.contains("puff") ||
                    lower.contains("spray") || lower.contains("nebul") ||
                    lower.contains("rotacap") -> INHALER
                lower.contains("drop") -> DROPS
                lower.contains("inject") || lower.contains("insulin") ||
                    lower.contains("vaccin") || lower.contains("vial") -> INJECTION
                lower.contains("syrup") || lower.contains("liquid") ||
                    lower.contains("tonic") || lower.contains("solution") ||
                    lower.contains("gargle") || lower.contains("mouthwash") ||
                    lower.contains("oil") -> SYRUP
                lower.contains("capsule") -> CAPSULE
                else -> TABLET
            }
        }

        fun dosagePresetsForType(typeLabel: String): List<String> {
            val trimmed = typeLabel.trim()
            fromRawOrNull(trimmed)?.let { return it.dosagePresets() }
            val lower = trimmed.lowercase()
            if (lower.isEmpty()) {
                return listOf("1 dose", "2 doses", "1/2 dose")
            }
            return when {
                lower.contains("inhaler") || lower.contains("puff") ||
                    lower.contains("rotacap") || lower.contains("nebul") ||
                    lower.contains("respule") -> INHALER.dosagePresets()
                lower.contains("spray") -> listOf(
                    "1 spray",
                    "2 sprays",
                    "1 spray each nostril",
                    "2 sprays each nostril"
                )
                lower.contains("cream") || lower.contains("ointment") ||
                    lower.contains("gel") || lower.contains("lotion") ||
                    lower.contains("balm") || lower.contains("paste") -> listOf(
                    "Thin layer",
                    "Pea-sized amount",
                    "1 fingertip unit",
                    "Apply on affected area"
                )
                lower.contains("powder") || lower.contains("sachet") ||
                    lower.contains("granule") || lower.contains("ors") -> listOf(
                    "1 sachet",
                    "1/2 sachet",
                    "1 scoop",
                    "1 teaspoon (5 g)",
                    "1 packet in water"
                )
                lower.contains("patch") || lower.contains("plaster") -> listOf(
                    "1 patch",
                    "1/2 patch"
                )
                lower.contains("lozenge") || lower.contains("chewable") ||
                    lower.contains("gum") -> listOf(
                    "1 lozenge",
                    "2 lozenges",
                    "1 chewable"
                )
                lower.contains("mouthwash") || lower.contains("gargle") ||
                    lower.contains("rinse") -> listOf(
                    "10 ml gargle",
                    "15 ml gargle",
                    "1 capful"
                )
                lower.contains("drop") -> DROPS.dosagePresets()
                lower.contains("oil") || lower.contains("tel") -> listOf(
                    "2-3 drops",
                    "5 ml",
                    "1 teaspoon",
                    "Apply gently"
                )
                lower.contains("syrup") || lower.contains("liquid") ||
                    lower.contains("tonic") || lower.contains("solution") ||
                    lower.contains("suspension") || lower.contains("elixir") -> SYRUP.dosagePresets()
                lower.contains("inject") || lower.contains("insulin") ||
                    lower.contains("vaccin") || lower.contains("vial") ||
                    lower.contains("ampoule") || lower.contains("pen") -> INJECTION.dosagePresets()
                lower.contains("suppositor") -> listOf(
                    "1 suppository",
                    "2 suppositories"
                )
                lower.contains("capsule") || lower == "cap" -> CAPSULE.dosagePresets()
                lower.contains("tablet") || lower == "tab" ||
                    lower.contains("pill") || lower.contains("goli") -> TABLET.dosagePresets()
                else -> listOf(
                    "1 $lower",
                    "2 $lower",
                    "1/2 $lower",
                    "1 dose"
                )
            }
        }
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

data class FamilyMember(
    val id: String,
    val name: String,
    val relation: String = "Family",
    val isDefault: Boolean = false
) {
    companion object {
        const val SELF_ID = "self"
        const val MOM_ID = "mom"
        const val DAD_ID = "dad"
        const val ALL_ID = "__all__"

        val DefaultMembers = listOf(
            FamilyMember(id = SELF_ID, name = "Self", relation = "Self", isDefault = true),
            FamilyMember(id = MOM_ID, name = "Mom", relation = "Parent", isDefault = true),
            FamilyMember(id = DAD_ID, name = "Dad", relation = "Parent", isDefault = true)
        )
    }
}

data class UserProfile(
    val displayName: String = "Patient",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val dob: String = "",
    val state: String = "",
    val city: String = "",
    val phone: String = "",
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val afternoonHour: Int = 13,
    val afternoonMinute: Int = 30,
    val eveningHour: Int = 18,
    val eveningMinute: Int = 0,
    val nightHour: Int = 21,
    val nightMinute: Int = 0
)

enum class GenderOption(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}

sealed interface TodayDoseFilter {
    data object All : TodayDoseFilter
    data class Slot(val slot: TimeSlot) : TodayDoseFilter
    data object Taken : TodayDoseFilter
}

data class Medicine(
    val id: String = "",
    val memberId: String = FamilyMember.SELF_ID,
    val name: String = "",
    val dosage: String = "",
    val form: String = MedicineForm.TABLET.name,
    val mealRelation: String = MealRelation.AFTER_MEAL.name,
    val notes: String = "",
    val morningEnabled: Boolean = true,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val morningMealRelation: String = mealRelation,
    val afternoonEnabled: Boolean = false,
    val afternoonHour: Int = 13,
    val afternoonMinute: Int = 30,
    val afternoonMealRelation: String = mealRelation,
    val eveningEnabled: Boolean = false,
    val eveningHour: Int = 18,
    val eveningMinute: Int = 0,
    val eveningMealRelation: String = mealRelation,
    val nightEnabled: Boolean = false,
    val nightHour: Int = 21,
    val nightMinute: Int = 0,
    val nightMealRelation: String = mealRelation,
    val startDateKey: String? = null,
    val endDateKey: String? = null,
    val recurrenceFrequency: String = RecurrenceFrequency.DAILY.name,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: String = "",
    val recurrenceEndType: String = RecurrenceEndType.NEVER.name,
    val recurrenceCount: Int? = null,
    val active: Boolean = true
) {
    fun formEnum(): MedicineForm = MedicineForm.fromRaw(form)
    fun isCustomForm(): Boolean = MedicineForm.fromRawOrNull(form) == null && form.isNotBlank()
    fun formDisplayName(): String =
        MedicineForm.fromRawOrNull(form)?.displayName ?: form.trim().ifBlank { MedicineForm.TABLET.displayName }
    fun formIconCategory(): MedicineForm =
        MedicineForm.fromRawOrNull(form) ?: MedicineForm.inferIconCategory(form)
    fun mealRelationEnum(): MealRelation = MealRelation.fromRaw(mealRelation)

    fun recurrenceRule(): RecurrenceRule = RecurrenceRule(
        frequency = RecurrenceFrequency.fromRaw(recurrenceFrequency),
        interval = recurrenceInterval.coerceAtLeast(1),
        daysOfWeek = parseDaysCsv(recurrenceDays),
        endType = RecurrenceEndType.fromRaw(recurrenceEndType, endDateKey),
        startDateKey = startDateKey?.takeIf { it.isNotBlank() },
        endDateKey = endDateKey?.takeIf { it.isNotBlank() },
        occurrenceCount = recurrenceCount?.takeIf { it > 0 }
    )

    fun hasDateRange(): Boolean {
        val rule = recurrenceRule()
        return rule.frequency != RecurrenceFrequency.DAILY ||
            rule.interval > 1 ||
            rule.endType != RecurrenceEndType.NEVER ||
            !startDateKey.isNullOrBlank() ||
            !endDateKey.isNullOrBlank()
    }

    fun isScheduledOnDate(dateKey: String = todayDateKey()): Boolean =
        isScheduledOnRecurrence(dateKey, recurrenceRule())

    fun isCourseEnded(todayKey: String = todayDateKey()): Boolean {
        val end = computeEffectiveEndDateKey(recurrenceRule()) ?: return false
        return todayKey > end
    }

    fun isCourseUpcoming(todayKey: String = todayDateKey()): Boolean {
        val start = startDateKey?.takeIf { it.isNotBlank() } ?: return false
        return todayKey < start
    }

    fun formattedDateRange(): String = formatRecurrenceSummary(recurrenceRule())

    fun mealRelationFor(slot: TimeSlot): MealRelation = when (slot) {
        TimeSlot.MORNING -> MealRelation.fromRaw(morningMealRelation)
        TimeSlot.AFTERNOON -> MealRelation.fromRaw(afternoonMealRelation)
        TimeSlot.EVENING -> MealRelation.fromRaw(eveningMealRelation)
        TimeSlot.NIGHT -> MealRelation.fromRaw(nightMealRelation)
    }

    fun isSlotEnabled(slot: TimeSlot): Boolean = when (slot) {
        TimeSlot.MORNING -> morningEnabled
        TimeSlot.AFTERNOON -> afternoonEnabled
        TimeSlot.EVENING -> eveningEnabled
        TimeSlot.NIGHT -> nightEnabled
    }

    fun hourFor(slot: TimeSlot): Int = when (slot) {
        TimeSlot.MORNING -> morningHour
        TimeSlot.AFTERNOON -> afternoonHour
        TimeSlot.EVENING -> eveningHour
        TimeSlot.NIGHT -> nightHour
    }

    fun minuteFor(slot: TimeSlot): Int = when (slot) {
        TimeSlot.MORNING -> morningMinute
        TimeSlot.AFTERNOON -> afternoonMinute
        TimeSlot.EVENING -> eveningMinute
        TimeSlot.NIGHT -> nightMinute
    }

    fun enabledSlots(): List<TimeSlot> = TimeSlot.entries.filter { isSlotEnabled(it) }
}

data class DoseLog(
    val id: String = "",
    val medicineId: String = "",
    val memberId: String = FamilyMember.SELF_ID,
    val memberName: String = "Self",
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
    val customFormLabel: String = "",
    val memberId: String = FamilyMember.SELF_ID,
    val mealRelation: MealRelation = MealRelation.AFTER_MEAL,
    val notes: String,
    val morningEnabled: Boolean,
    val morningHour: Int,
    val morningMinute: Int,
    val morningMealRelation: MealRelation = mealRelation,
    val afternoonEnabled: Boolean,
    val afternoonHour: Int,
    val afternoonMinute: Int,
    val afternoonMealRelation: MealRelation = mealRelation,
    val eveningEnabled: Boolean,
    val eveningHour: Int,
    val eveningMinute: Int,
    val eveningMealRelation: MealRelation = mealRelation,
    val nightEnabled: Boolean,
    val nightHour: Int,
    val nightMinute: Int,
    val nightMealRelation: MealRelation = mealRelation,
    val startDateKey: String? = null,
    val endDateKey: String? = null,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: Set<Int> = emptySet(),
    val recurrenceEndType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val recurrenceCount: Int? = null,
    val active: Boolean = true
)

data class ScheduledDoseItem(
    val medicine: Medicine,
    val memberName: String = "Self",
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

    val mealRelation: MealRelation
        get() = medicine.mealRelationFor(slot)
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
            val display = SimpleDateFormat("EEE, dd-MM-yyyy", Locale.US)
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

fun formatDateKeyDmy(dateKey: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val out = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val parsed = parser.parse(dateKey)
        if (parsed != null) out.format(parsed) else dateKey
    } catch (_: Exception) {
        dateKey
    }
}

fun dateKeyToUtcMillis(dateKey: String): Long? {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        fmt.parse(dateKey)?.time
    } catch (_: Exception) {
        null
    }
}

fun utcMillisToDateKey(utcMillis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(java.util.Date(utcMillis))
}

fun todayUtcMillis(): Long {
    return dateKeyToUtcMillis(todayDateKey()) ?: System.currentTimeMillis()
}

fun shiftDateKey(dateKey: String, days: Int): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = fmt.parse(dateKey) ?: return dateKey
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, days)
        }
        fmt.format(cal.time)
    } catch (_: Exception) {
        dateKey
    }
}

fun formatEpochDmy(epochMillis: Long): String {
    return try {
        SimpleDateFormat("dd-MM-yyyy", Locale.US).format(java.util.Date(epochMillis))
    } catch (_: Exception) {
        ""
    }
}

fun normalizePhoneDigits(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length > 10 && digits.startsWith("91") && digits.length == 12) {
        digits.takeLast(10)
    } else if (digits.length > 10 && digits.startsWith("0") && digits.length == 11) {
        digits.takeLast(10)
    } else {
        digits
    }
}

fun isValidDmyDob(dob: String): Boolean {
    val trimmed = dob.trim()
    if (!Regex("^\\d{2}-\\d{2}-\\d{4}$").matches(trimmed)) return false
    return try {
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            isLenient = false
        }
        val parsed = fmt.parse(trimmed) ?: return false
        parsed.time <= System.currentTimeMillis()
    } catch (_: Exception) {
        false
    }
}

enum class RecurrenceFrequency(
    val label: String,
    val unitSingular: String,
    val unitPlural: String
) {
    NONE("Does not repeat", "day", "days"),
    DAILY("Every day", "day", "days"),
    WEEKLY("Every week", "week", "weeks"),
    MONTHLY("Every month", "month", "months"),
    YEARLY("Every year", "year", "years");

    fun unitLabel(interval: Int): String = if (interval <= 1) unitSingular else unitPlural

    companion object {
        fun fromRaw(raw: String?): RecurrenceFrequency =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY
    }
}

enum class RecurrenceEndType(val label: String) {
    NEVER("Never"),
    ON_DATE("On"),
    AFTER_OCCURRENCES("After");

    companion object {
        fun fromRaw(raw: String?, fallbackEndDateKey: String? = null): RecurrenceEndType {
            val matched = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            if (matched != null) return matched
            return if (!fallbackEndDateKey.isNullOrBlank()) ON_DATE else NEVER
        }
    }
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val interval: Int = 1,
    val daysOfWeek: Set<Int> = emptySet(),
    val endType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val startDateKey: String? = null,
    val endDateKey: String? = null,
    val occurrenceCount: Int? = null
)

val WEEKDAY_OPTIONS: List<Pair<Int, String>> = listOf(
    Calendar.SUNDAY to "S",
    Calendar.MONDAY to "M",
    Calendar.TUESDAY to "T",
    Calendar.WEDNESDAY to "W",
    Calendar.THURSDAY to "T",
    Calendar.FRIDAY to "F",
    Calendar.SATURDAY to "S"
)

fun weekdayShortName(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.SUNDAY -> "Sun"
    Calendar.MONDAY -> "Mon"
    Calendar.TUESDAY -> "Tue"
    Calendar.WEDNESDAY -> "Wed"
    Calendar.THURSDAY -> "Thu"
    Calendar.FRIDAY -> "Fri"
    Calendar.SATURDAY -> "Sat"
    else -> ""
}

fun weekdayFullName(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.SUNDAY -> "Sunday"
    Calendar.MONDAY -> "Monday"
    Calendar.TUESDAY -> "Tuesday"
    Calendar.WEDNESDAY -> "Wednesday"
    Calendar.THURSDAY -> "Thursday"
    Calendar.FRIDAY -> "Friday"
    Calendar.SATURDAY -> "Saturday"
    else -> ""
}

fun parseDaysCsv(csv: String?): Set<Int> {
    if (csv.isNullOrBlank()) return emptySet()
    return csv.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
        .toSet()
}

fun toDaysCsv(days: Set<Int>): String =
    days.filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
        .sorted()
        .joinToString(",")

fun dayOfWeekForDateKey(dateKey: String): Int {
    val utc = dateKeyToUtcCalendar(dateKey) ?: Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    return utc.get(Calendar.DAY_OF_WEEK)
}

private fun dateKeyToUtcCalendar(dateKey: String): Calendar? {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = fmt.parse(dateKey.trim()) ?: return null
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    } catch (_: Exception) {
        null
    }
}

private fun utcCalendarToDateKey(cal: Calendar): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(cal.time)
}

private fun daysBetweenUtc(startCal: Calendar, endCal: Calendar): Long {
    val millisPerDay = 86_400_000L
    return (endCal.timeInMillis - startCal.timeInMillis) / millisPerDay
}

fun computeEffectiveEndDateKey(rule: RecurrenceRule): String? {
    val startKey = rule.startDateKey?.takeIf { it.isNotBlank() }
    return when (rule.frequency) {
        RecurrenceFrequency.NONE -> startKey
        else -> when (rule.endType) {
            RecurrenceEndType.NEVER -> null
            RecurrenceEndType.ON_DATE -> rule.endDateKey?.takeIf { it.isNotBlank() }
            RecurrenceEndType.AFTER_OCCURRENCES -> {
                val count = (rule.occurrenceCount ?: 1).coerceIn(1, 3650)
                val anchorKey = startKey ?: todayDateKey()
                val anchorCal = dateKeyToUtcCalendar(anchorKey) ?: return anchorKey
                val interval = rule.interval.coerceAtLeast(1)
                when (rule.frequency) {
                    RecurrenceFrequency.NONE -> anchorKey
                    RecurrenceFrequency.DAILY -> {
                        val cal = (anchorCal.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, (count - 1) * interval)
                        }
                        utcCalendarToDateKey(cal)
                    }
                    RecurrenceFrequency.WEEKLY -> {
                        val activeDays = rule.daysOfWeek.ifEmpty {
                            setOf(anchorCal.get(Calendar.DAY_OF_WEEK))
                        }
                        var found = 0
                        val cursor = anchorCal.clone() as Calendar
                        var lastMatched: Calendar = anchorCal
                        var safety = 0
                        while (found < count && safety < 20_000) {
                            val candDow = cursor.get(Calendar.DAY_OF_WEEK)
                            if (candDow in activeDays) {
                                val anchorDow = anchorCal.get(Calendar.DAY_OF_WEEK)
                                val anchorWeekStart = daysBetweenUtc(
                                    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = 0 },
                                    anchorCal
                                ) - (anchorDow - Calendar.SUNDAY)
                                val cursorWeekStart = daysBetweenUtc(
                                    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = 0 },
                                    cursor
                                ) - (candDow - Calendar.SUNDAY)
                                val weekDiff = (cursorWeekStart - anchorWeekStart) / 7L
                                if (weekDiff >= 0 && weekDiff % interval == 0L) {
                                    found++
                                    lastMatched = cursor.clone() as Calendar
                                }
                            }
                            cursor.add(Calendar.DAY_OF_YEAR, 1)
                            safety++
                        }
                        utcCalendarToDateKey(lastMatched)
                    }
                    RecurrenceFrequency.MONTHLY -> {
                        val targetDay = anchorCal.get(Calendar.DAY_OF_MONTH)
                        val cal = (anchorCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            add(Calendar.MONTH, (count - 1) * interval)
                            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                            set(Calendar.DAY_OF_MONTH, minOf(targetDay, maxDay))
                        }
                        utcCalendarToDateKey(cal)
                    }
                    RecurrenceFrequency.YEARLY -> {
                        val targetDay = anchorCal.get(Calendar.DAY_OF_MONTH)
                        val targetMonth = anchorCal.get(Calendar.MONTH)
                        val cal = (anchorCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            add(Calendar.YEAR, (count - 1) * interval)
                            set(Calendar.MONTH, targetMonth)
                            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                            set(Calendar.DAY_OF_MONTH, minOf(targetDay, maxDay))
                        }
                        utcCalendarToDateKey(cal)
                    }
                }
            }
        }
    }
}

fun isScheduledOnRecurrence(dateKey: String, rule: RecurrenceRule): Boolean {
    val startKey = rule.startDateKey?.takeIf { it.isNotBlank() }
    val interval = rule.interval.coerceAtLeast(1)

    if (rule.frequency == RecurrenceFrequency.DAILY &&
        interval == 1 &&
        rule.endType == RecurrenceEndType.NEVER &&
        startKey == null
    ) {
        return true
    }

    val effectiveStart = startKey ?: todayDateKey()
    if (dateKey < effectiveStart) return false

    val effectiveEnd = computeEffectiveEndDateKey(rule)
    if (effectiveEnd != null && dateKey > effectiveEnd) return false

    if (rule.frequency == RecurrenceFrequency.NONE) {
        return dateKey == effectiveStart
    }

    val anchorCal = dateKeyToUtcCalendar(effectiveStart) ?: return true
    val candCal = dateKeyToUtcCalendar(dateKey) ?: return false

    return when (rule.frequency) {
        RecurrenceFrequency.NONE -> dateKey == effectiveStart
        RecurrenceFrequency.DAILY -> {
            val diff = daysBetweenUtc(anchorCal, candCal)
            diff >= 0L && diff % interval == 0L
        }
        RecurrenceFrequency.WEEKLY -> {
            val activeDays = rule.daysOfWeek.ifEmpty {
                setOf(anchorCal.get(Calendar.DAY_OF_WEEK))
            }
            val candDow = candCal.get(Calendar.DAY_OF_WEEK)
            if (candDow !in activeDays) return false
            val epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = 0 }
            val anchorDow = anchorCal.get(Calendar.DAY_OF_WEEK)
            val anchorWeekStart = daysBetweenUtc(epoch, anchorCal) - (anchorDow - Calendar.SUNDAY)
            val candWeekStart = daysBetweenUtc(epoch, candCal) - (candDow - Calendar.SUNDAY)
            val weekDiff = (candWeekStart - anchorWeekStart) / 7L
            weekDiff >= 0L && weekDiff % interval == 0L
        }
        RecurrenceFrequency.MONTHLY -> {
            val monthsDiff = (candCal.get(Calendar.YEAR) - anchorCal.get(Calendar.YEAR)) * 12 +
                (candCal.get(Calendar.MONTH) - anchorCal.get(Calendar.MONTH))
            if (monthsDiff < 0 || monthsDiff % interval != 0) return false
            val targetDay = anchorCal.get(Calendar.DAY_OF_MONTH)
            val maxDay = candCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            candCal.get(Calendar.DAY_OF_MONTH) == minOf(targetDay, maxDay)
        }
        RecurrenceFrequency.YEARLY -> {
            val yearsDiff = candCal.get(Calendar.YEAR) - anchorCal.get(Calendar.YEAR)
            if (yearsDiff < 0 || yearsDiff % interval != 0) return false
            if (candCal.get(Calendar.MONTH) != anchorCal.get(Calendar.MONTH)) return false
            val targetDay = anchorCal.get(Calendar.DAY_OF_MONTH)
            val maxDay = candCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            candCal.get(Calendar.DAY_OF_MONTH) == minOf(targetDay, maxDay)
        }
    }
}

fun computeNextScheduledDateKey(fromDateKey: String, rule: RecurrenceRule): String? {
    val startKey = rule.startDateKey?.takeIf { it.isNotBlank() }
    val searchStartKey = if (startKey != null && fromDateKey < startKey) startKey else fromDateKey
    val effectiveEnd = computeEffectiveEndDateKey(rule)
    if (effectiveEnd != null && searchStartKey > effectiveEnd) return null

    if (rule.frequency == RecurrenceFrequency.NONE) {
        val anchor = startKey ?: fromDateKey
        return if (searchStartKey <= anchor) anchor else null
    }

    val searchCal = dateKeyToUtcCalendar(searchStartKey) ?: return null
    val maxSteps = when (rule.frequency) {
        RecurrenceFrequency.DAILY -> (rule.interval.coerceAtLeast(1) + 2).coerceAtMost(400)
        RecurrenceFrequency.WEEKLY -> (rule.interval.coerceAtLeast(1) * 7 + 7).coerceAtMost(800)
        RecurrenceFrequency.MONTHLY -> (rule.interval.coerceAtLeast(1) * 32 + 35).coerceAtMost(1200)
        RecurrenceFrequency.YEARLY -> (rule.interval.coerceAtLeast(1) * 366 + 5).coerceAtMost(2000)
        RecurrenceFrequency.NONE -> 1
    }

    repeat(maxSteps) {
        val candKey = utcCalendarToDateKey(searchCal)
        if (effectiveEnd != null && candKey > effectiveEnd) return null
        if (isScheduledOnRecurrence(candKey, rule)) return candKey
        searchCal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return null
}

fun formatRecurrenceSummary(rule: RecurrenceRule): String {
    val interval = rule.interval.coerceAtLeast(1)
    val startFormatted = rule.startDateKey?.takeIf { it.isNotBlank() }?.let(::formatDateKeyDmy)
    if (rule.frequency == RecurrenceFrequency.NONE) {
        return if (startFormatted != null) "Does not repeat • $startFormatted" else "Does not repeat"
    }

    val freqPart = when (rule.frequency) {
        RecurrenceFrequency.NONE -> "Does not repeat"
        RecurrenceFrequency.DAILY -> if (interval == 1) "Every day" else "Every $interval days"
        RecurrenceFrequency.WEEKLY -> {
            val base = if (interval == 1) "Every week" else "Every $interval weeks"
            val days = rule.daysOfWeek
                .ifEmpty {
                    rule.startDateKey?.takeIf { it.isNotBlank() }?.let { setOf(dayOfWeekForDateKey(it)) }
                        ?: emptySet()
                }
                .sorted()
                .map(::weekdayShortName)
                .filter { it.isNotBlank() }
            if (days.isEmpty()) base else "$base on ${days.joinToString(", ")}"
        }
        RecurrenceFrequency.MONTHLY -> {
            val base = if (interval == 1) "Every month" else "Every $interval months"
            val dayNum = rule.startDateKey?.takeIf { it.isNotBlank() }
                ?.let { dateKeyToUtcCalendar(it)?.get(Calendar.DAY_OF_MONTH) }
            if (dayNum != null) "$base on day $dayNum" else base
        }
        RecurrenceFrequency.YEARLY -> if (interval == 1) "Every year" else "Every $interval years"
    }

    val endPart = when (rule.endType) {
        RecurrenceEndType.NEVER -> null
        RecurrenceEndType.ON_DATE -> rule.endDateKey?.takeIf { it.isNotBlank() }
            ?.let { "Until ${formatDateKeyDmy(it)}" }
        RecurrenceEndType.AFTER_OCCURRENCES -> {
            val n = (rule.occurrenceCount ?: 1).coerceAtLeast(1)
            val computedEnd = computeEffectiveEndDateKey(rule)?.let(::formatDateKeyDmy)
            val timesLabel = if (n == 1) "1 time" else "$n times"
            if (computedEnd != null) "$timesLabel (until $computedEnd)" else timesLabel
        }
    }

    return listOfNotNull(freqPart, endPart).joinToString(" • ")
}



