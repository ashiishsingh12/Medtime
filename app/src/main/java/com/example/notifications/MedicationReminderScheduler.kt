package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.MealRelation
import com.example.model.Medicine
import com.example.model.TimeSlot
import java.util.Calendar

object MedicationReminderScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun syncAllAlarms(context: Context, medicines: List<Medicine>) {
        val appContext = context.applicationContext
        val previousEntries = AlarmScheduleCache.getEntries(appContext)
        previousEntries.forEach { oldEntry ->
            cancelPendingIntent(appContext, oldEntry.requestCode)
        }

        val currentEntries = AlarmScheduleCache.saveFromMedicines(appContext, medicines)
        currentEntries.forEach { entry ->
            scheduleNextDailyAlarm(appContext, entry)
        }
    }

    fun rescheduleAllFromCache(context: Context) {
        val appContext = context.applicationContext
        AlarmScheduleCache.getEntries(appContext).forEach { entry ->
            scheduleNextDailyAlarm(appContext, entry)
        }
    }

    fun cancelAllAlarms(context: Context) {
        val appContext = context.applicationContext
        AlarmScheduleCache.getEntries(appContext).forEach { entry ->
            cancelPendingIntent(appContext, entry.requestCode)
        }
        AlarmScheduleCache.clear(appContext)
    }

    fun scheduleNextDailyAlarm(
        context: Context,
        entry: CachedReminderEntry,
        forceTomorrow: Boolean = false
    ) {
        val triggerAtMillis = computeNextTriggerMillis(
            hour = entry.hour,
            minute = entry.minute,
            forceTomorrow = forceTomorrow
        )
        val intent = buildReceiverIntent(
            context = context,
            medicineId = entry.medicineId,
            medicineName = entry.medicineName,
            dosage = entry.dosage,
            mealRelation = entry.mealRelation,
            slot = entry.slot,
            hour = entry.hour,
            minute = entry.minute,
            isSnooze = false
        )
        scheduleExactOrFallback(
            context = context,
            requestCode = entry.requestCode,
            triggerAtMillis = triggerAtMillis,
            intent = intent
        )
    }

    fun scheduleSnoozeAlarm(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        mealRelation: MealRelation,
        slot: TimeSlot,
        hour: Int,
        minute: Int,
        delayMinutes: Int = 10
    ) {
        val triggerAtMillis = System.currentTimeMillis() + delayMinutes * 60_000L
        val snoozeRequestCode = ("snooze_${medicineId}_${slot.name}".hashCode() and 0x7FFFFFFF)
        val intent = buildReceiverIntent(
            context = context,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            mealRelation = mealRelation,
            slot = slot,
            hour = hour,
            minute = minute,
            isSnooze = true
        )
        scheduleExactOrFallback(
            context = context,
            requestCode = snoozeRequestCode,
            triggerAtMillis = triggerAtMillis,
            intent = intent
        )
    }

    private fun scheduleExactOrFallback(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        intent: Intent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelPendingIntent(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val existing = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    private fun buildReceiverIntent(
        context: Context,
        medicineId: String,
        medicineName: String,
        dosage: String,
        mealRelation: MealRelation,
        slot: TimeSlot,
        hour: Int,
        minute: Int,
        isSnooze: Boolean
    ): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MEDICINE_ID, medicineId)
            putExtra(ReminderReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(ReminderReceiver.EXTRA_DOSAGE, dosage)
            putExtra(ReminderReceiver.EXTRA_MEAL_RELATION, mealRelation.name)
            putExtra(ReminderReceiver.EXTRA_SLOT, slot.name)
            putExtra(ReminderReceiver.EXTRA_HOUR, hour)
            putExtra(ReminderReceiver.EXTRA_MINUTE, minute)
            putExtra(ReminderReceiver.EXTRA_IS_SNOOZE, isSnooze)
        }
    }

    private fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        forceTomorrow: Boolean
    ): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (forceTomorrow || !target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
