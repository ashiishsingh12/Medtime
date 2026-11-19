package com.example.notifications

import android.content.Context
import com.example.model.MealRelation
import com.example.model.Medicine
import com.example.model.TimeSlot
import org.json.JSONArray
import org.json.JSONObject

data class CachedReminderEntry(
    val medicineId: String,
    val medicineName: String,
    val dosage: String,
    val mealRelation: MealRelation,
    val slot: TimeSlot,
    val hour: Int,
    val minute: Int
) {
    val requestCode: Int
        get() = "${medicineId}_${slot.name}".hashCode() and 0x7FFFFFFF
}

object AlarmScheduleCache {
    private const val PREFS_NAME = "medtime_alarm_cache"
    private const val KEY_ENTRIES = "scheduled_entries"

    fun saveFromMedicines(context: Context, medicines: List<Medicine>): List<CachedReminderEntry> {
        val entries = mutableListOf<CachedReminderEntry>()
        medicines.filter { it.active && it.id.isNotBlank() }.forEach { med ->
            TimeSlot.entries.forEach { slot ->
                if (med.isSlotEnabled(slot)) {
                    entries.add(
                        CachedReminderEntry(
                            medicineId = med.id,
                            medicineName = med.name,
                            dosage = med.dosage,
                            mealRelation = med.mealRelationEnum(),
                            slot = slot,
                            hour = med.hourFor(slot),
                            minute = med.minuteFor(slot)
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
                    add(
                        CachedReminderEntry(
                            medicineId = obj.getString("medicineId"),
                            medicineName = obj.getString("medicineName"),
                            dosage = obj.getString("dosage"),
                            mealRelation = MealRelation.fromRaw(obj.optString("mealRelation")),
                            slot = TimeSlot.fromRaw(obj.optString("slot")),
                            hour = obj.getInt("hour"),
                            minute = obj.getInt("minute")
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
                    put("medicineName", entry.medicineName)
                    put("dosage", entry.dosage)
                    put("mealRelation", entry.mealRelation.name)
                    put("slot", entry.slot.name)
                    put("hour", entry.hour)
                    put("minute", entry.minute)
                }
            )
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }
}
