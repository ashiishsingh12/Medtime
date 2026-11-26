package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.model.MealRelation
import com.example.model.RecurrenceRule
import com.example.model.TimeSlot
import com.example.model.isScheduledOnRecurrence
import com.example.model.todayDateKey

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(EXTRA_MEDICINE_ID) ?: return
        val memberName = intent.getStringExtra(EXTRA_MEMBER_NAME)?.ifBlank { "Self" } ?: "Self"
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: "1 dose"
        val mealRelation = MealRelation.fromRaw(intent.getStringExtra(EXTRA_MEAL_RELATION))
        val slot = TimeSlot.fromRaw(intent.getStringExtra(EXTRA_SLOT))
        val hour = intent.getIntExtra(EXTRA_HOUR, slot.defaultHour)
        val minute = intent.getIntExtra(EXTRA_MINUTE, slot.defaultMinute)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        val cached = AlarmScheduleCache.findEntry(context, medicineId, slot)
        val startDateKey = intent.getStringExtra(EXTRA_START_DATE_KEY)?.ifBlank { null } ?: cached?.startDateKey
        val endDateKey = intent.getStringExtra(EXTRA_END_DATE_KEY)?.ifBlank { null } ?: cached?.endDateKey
        val todayKey = todayDateKey()

        val activeRule = cached?.recurrenceRule() ?: RecurrenceRule(
            startDateKey = startDateKey,
            endDateKey = endDateKey
        )

        if (!isSnooze && !isScheduledOnRecurrence(todayKey, activeRule)) {
            if (cached != null) {
                MedicationReminderScheduler.scheduleNextDailyAlarm(
                    context = context,
                    entry = cached,
                    forceTomorrow = true
                )
            }
            return
        }

        MedicationNotificationHelper.showReminderNotification(
            context = context,
            medicineId = medicineId,
            memberName = memberName,
            medicineName = medicineName,
            dosage = dosage,
            mealRelation = mealRelation,
            slot = slot,
            hour = hour,
            minute = minute
        )

        if (!isSnooze && cached != null) {
            MedicationReminderScheduler.scheduleNextDailyAlarm(
                context = context,
                entry = cached,
                forceTomorrow = true
            )
        }
    }

    companion object {
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEMBER_NAME = "extra_member_name"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_MEAL_RELATION = "extra_meal_relation"
        const val EXTRA_SLOT = "extra_slot"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
        const val EXTRA_START_DATE_KEY = "extra_start_date_key"
        const val EXTRA_END_DATE_KEY = "extra_end_date_key"
    }
}
