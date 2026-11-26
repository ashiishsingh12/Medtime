package com.example.notifications

import android.content.Context
import com.example.model.MealRelation
import com.example.model.Medicine
import com.example.model.RecurrenceEndType
import com.example.model.RecurrenceFrequency
import com.example.model.RecurrenceRule
import com.example.model.TimeSlot
import com.example.model.parseDaysCsv
import com.example.model.todayDateKey
import org.json.JSONArray
import org.json.JSONObject

data class CachedReminderEntry(
    val medicineId: String,
    val memberName: String,
    val medicineName: String,
    val dosage: String,
    val mealRelation: MealRelation,
    val slot: TimeSlot,
    val hour: Int,
    val minute: Int,
    val startDateKey: String? = null,
    val endDateKey: String? = null,
    val recurrenceFrequency: String = RecurrenceFrequency.DAILY.name,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: String = "",
    val recurrenceEndType: String = RecurrenceEndType.NEVER.name,
    val recurrenceCount: Int? = null
) {
    val requestCode: Int
        get() = "${medicineId}_${slot.name}".hashCode() and 0x7FFFFFFF

    fun recurrenceRule(): RecurrenceRule = RecurrenceRule(
        frequency = RecurrenceFrequency.fromRaw(recurrenceFrequency),
        interval = recurrenceInterval.coerceAtLeast(1),
        daysOfWeek = parseDaysCsv(recurrenceDays),
        endType = RecurrenceEndType.fromRaw(recurrenceEndType, endDateKey),
        startDateKey = startDateKey?.takeIf { it.isNotBlank() },
        endDateKey = endDateKey?.takeIf { it.isNotBlank() },
        occurrenceCount = recurrenceCount?.takeIf { it > 0 }
    )
}

object AlarmScheduleCache {
    private const val PREFS_NAME = "medtime_alarm_cache"
    private const val KEY_ENTRIES = "scheduled_entries"

    fun saveFromMedicines(
        context: Context,
        medicines: List<Medicine>,
        memberNamesById: Map<String, String> = emptyMap()
    ): List<CachedReminderEntry> {
        val entries = mutableListOf<CachedReminderEntry>()
        val todayKey = todayDateKey()
        medicines
            .filter { it.active && it.id.isNotBlank() && !it.isCourseEnded(todayKey) }
            .forEach { med ->
                val memberName = memberNamesById[med.memberId] ?: "Self"
                TimeSlot.entries.forEach { slot ->
                    if (med.isSlotEnabled(slot)) {
                        entries.add(
                            CachedReminderEntry(
                                medicineId = med.id,
                                memberName = memberName,
                                medicineName = med.name,
                                dosage = med.dosage,
                                mealRelation = med.mealRelationFor(slot),
                                slot = slot,
                                hour = med.hourFor(slot),
                                minute = med.minuteFor(slot),
                                startDateKey = med.startDateKey,
                                endDateKey = med.endDateKey,
                                recurrenceFrequency = med.recurrenceFrequency,
                                recurrenceInterval = med.recurrenceInterval,
                                recurrenceDays = med.recurrenceDays,
                                recurrenceEndType = med.recurrenceEndType,
                                recurrenceCount = med.recurrenceCount
                            )
                        )
                    }
                }
            }
        saveEntries(context, entries)
        return entries
    }

    fun getEntries(context: Context): List<CachedReminderEntry> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val endKey = obj.optString("endDateKey", "").ifBlank { null }
                    val rawCount = obj.optInt("recurrenceCount", -1)
                    add(
                        CachedReminderEntry(
                            medicineId = obj.getString("medicineId"),
                            memberName = obj.optString("memberName", "Self").ifBlank { "Self" },
                            medicineName = obj.getString("medicineName"),
                            dosage = obj.getString("dosage"),
                            mealRelation = MealRelation.fromRaw(obj.optString("mealRelation")),
                            slot = TimeSlot.fromRaw(obj.optString("slot")),
                            hour = obj.getInt("hour"),
                            minute = obj.getInt("minute"),
                            startDateKey = obj.optString("startDateKey", "").ifBlank { null },
                            endDateKey = endKey,
                            recurrenceFrequency = obj.optString(
                                "recurrenceFrequency",
                                RecurrenceFrequency.DAILY.name
                            ).ifBlank { RecurrenceFrequency.DAILY.name },
                            recurrenceInterval = obj.optInt("recurrenceInterval", 1).coerceAtLeast(1),
                            recurrenceDays = obj.optString("recurrenceDays", ""),
                            recurrenceEndType = obj.optString(
                                "recurrenceEndType",
                                if (endKey != null) RecurrenceEndType.ON_DATE.name else RecurrenceEndType.NEVER.name
                            ),
                            recurrenceCount = if (rawCount > 0) rawCount else null
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun findEntry(context: Context, medicineId: String, slot: TimeSlot): CachedReminderEntry? {
        return getEntries(context).firstOrNull { it.medicineId == medicineId && it.slot == slot }
    }

    fun clear(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private fun saveEntries(context: Context, entries: List<CachedReminderEntry>) {
        val arr = JSONArray()
        entries.forEach { entry ->
            arr.put(
                JSONObject().apply {
                    put("medicineId", entry.medicineId)
                    put("memberName", entry.memberName)
                    put("medicineName", entry.medicineName)
                    put("dosage", entry.dosage)
                    put("mealRelation", entry.mealRelation.name)
                    put("slot", entry.slot.name)
                    put("hour", entry.hour)
                    put("minute", entry.minute)
                    put("startDateKey", entry.startDateKey.orEmpty())
                    put("endDateKey", entry.endDateKey.orEmpty())
                    put("recurrenceFrequency", entry.recurrenceFrequency)
                    put("recurrenceInterval", entry.recurrenceInterval)
                    put("recurrenceDays", entry.recurrenceDays)
                    put("recurrenceEndType", entry.recurrenceEndType)
                    put("recurrenceCount", entry.recurrenceCount ?: -1)
                }
            )
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }
}
