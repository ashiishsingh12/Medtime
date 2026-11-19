package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.model.MealRelation
import com.example.model.TimeSlot

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: "1 dose"
        val mealRelation = MealRelation.fromRaw(intent.getStringExtra(EXTRA_MEAL_RELATION))
        val slot = TimeSlot.fromRaw(intent.getStringExtra(EXTRA_SLOT))
        val hour = intent.getIntExtra(EXTRA_HOUR, slot.defaultHour)
        val minute = intent.getIntExtra(EXTRA_MINUTE, slot.defaultMinute)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        MedicationNotificationHelper.showReminderNotification(
            context = context,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            mealRelation = mealRelation,
            slot = slot,
            hour = hour,
            minute = minute
        )

        if (!isSnooze) {
            val cached = AlarmScheduleCache.findEntry(context, medicineId, slot)
            if (cached != null) {
                MedicationReminderScheduler.scheduleNextDailyAlarm(
                    context = context,
                    entry = cached,
                    forceTomorrow = true
                )
            }
        }
    }

    companion object {
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_MEAL_RELATION = "extra_meal_relation"
        const val EXTRA_SLOT = "extra_slot"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }
}
