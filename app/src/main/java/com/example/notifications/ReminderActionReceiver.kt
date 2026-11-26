package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.MedicationRepository
import com.example.model.DoseStatus
import com.example.model.MealRelation
import com.example.model.Medicine
import com.example.model.TimeSlot
import com.example.model.todayDateKey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        val medicineId = intent.getStringExtra(ReminderReceiver.EXTRA_MEDICINE_ID) ?: return
        val memberName = intent.getStringExtra(ReminderReceiver.EXTRA_MEMBER_NAME)?.ifBlank { "Self" } ?: "Self"
        val medicineName = intent.getStringExtra(ReminderReceiver.EXTRA_MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(ReminderReceiver.EXTRA_DOSAGE) ?: "1 dose"
        val mealRelation = MealRelation.fromRaw(intent.getStringExtra(ReminderReceiver.EXTRA_MEAL_RELATION))
        val slot = TimeSlot.fromRaw(intent.getStringExtra(ReminderReceiver.EXTRA_SLOT))
        val hour = intent.getIntExtra(ReminderReceiver.EXTRA_HOUR, slot.defaultHour)
        val minute = intent.getIntExtra(ReminderReceiver.EXTRA_MINUTE, slot.defaultMinute)

        when (intent.action) {
            ACTION_SNOOZE -> {
                MedicationReminderScheduler.scheduleSnoozeAlarm(
                    context = context,
                    medicineId = medicineId,
                    memberName = memberName,
                    medicineName = medicineName,
                    dosage = dosage,
                    mealRelation = mealRelation,
                    slot = slot,
                    hour = hour,
                    minute = minute,
                    delayMinutes = 10
                )
            }

            ACTION_MARK_TAKEN -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val uid = Firebase.auth.currentUser?.uid
                        if (!uid.isNullOrBlank()) {
                            val repo = MedicationRepository(context.applicationContext)
                            val placeholderMed = Medicine(
                                id = medicineId,
                                name = medicineName,
                                dosage = dosage,
                                morningHour = if (slot == TimeSlot.MORNING) hour else 8,
                                morningMinute = if (slot == TimeSlot.MORNING) minute else 0,
                                afternoonHour = if (slot == TimeSlot.AFTERNOON) hour else 13,
                                afternoonMinute = if (slot == TimeSlot.AFTERNOON) minute else 30,
                                eveningHour = if (slot == TimeSlot.EVENING) hour else 18,
                                eveningMinute = if (slot == TimeSlot.EVENING) minute else 0,
                                nightHour = if (slot == TimeSlot.NIGHT) hour else 21,
                                nightMinute = if (slot == TimeSlot.NIGHT) minute else 0
                            )
                            repo.markDoseStatus(
                                userId = uid,
                                medicine = placeholderMed,
                                slot = slot,
                                dateKey = todayDateKey(),
                                status = DoseStatus.TAKEN,
                                memberName = memberName
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
