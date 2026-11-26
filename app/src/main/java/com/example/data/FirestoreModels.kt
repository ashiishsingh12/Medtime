package com.example.data

import androidx.annotation.Keep
import com.example.model.DoseLog
import com.example.model.DoseStatus
import com.example.model.FamilyMember
import com.example.model.MealRelation
import com.example.model.Medicine
import com.example.model.MedicineForm
import com.example.model.RecurrenceEndType
import com.example.model.RecurrenceFrequency
import com.example.model.TimeSlot
import com.example.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Patient",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val dob: String = "",
    val state: String = "",
    val city: String = "",
    val phone: String = "",
    val morningHour: Long = 8L,
    val morningMinute: Long = 0L,
    val afternoonHour: Long = 13L,
    val afternoonMinute: Long = 30L,
    val eveningHour: Long = 18L,
    val eveningMinute: Long = 0L,
    val nightHour: Long = 21L,
    val nightMinute: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): UserProfile = UserProfile(
        displayName = displayName.ifBlank { "Patient" },
        email = email,
        firstName = firstName,
        lastName = lastName,
        gender = gender,
        dob = dob,
        state = state,
        city = city,
        phone = phone,
        morningHour = morningHour.toInt().coerceIn(0, 23),
        morningMinute = morningMinute.toInt().coerceIn(0, 59),
        afternoonHour = afternoonHour.toInt().coerceIn(0, 23),
        afternoonMinute = afternoonMinute.toInt().coerceIn(0, 59),
        eveningHour = eveningHour.toInt().coerceIn(0, 23),
        eveningMinute = eveningMinute.toInt().coerceIn(0, 59),
        nightHour = nightHour.toInt().coerceIn(0, 23),
        nightMinute = nightMinute.toInt().coerceIn(0, 59)
    )
}

@Keep
@IgnoreExtraProperties
data class FamilyMemberDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val relation: String = "Family",
    val sortOrder: Long = 0L,
    val defaultMember: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): FamilyMember = FamilyMember(
        id = id,
        name = name.ifBlank { "Member" },
        relation = relation.ifBlank { "Family" },
        isDefault = defaultMember
    )
}

@Keep
@IgnoreExtraProperties
data class MedicineDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberId: String = FamilyMember.SELF_ID,
    val name: String = "",
    val dosage: String = "",
    val form: String = MedicineForm.TABLET.name,
    val mealRelation: String = MealRelation.AFTER_MEAL.name,
    val notes: String = "",
    val morningEnabled: Boolean = true,
    val morningHour: Long = 8L,
    val morningMinute: Long = 0L,
    val morningMealRelation: String = MealRelation.AFTER_MEAL.name,
    val afternoonEnabled: Boolean = false,
    val afternoonHour: Long = 13L,
    val afternoonMinute: Long = 30L,
    val afternoonMealRelation: String = MealRelation.AFTER_MEAL.name,
    val eveningEnabled: Boolean = false,
    val eveningHour: Long = 18L,
    val eveningMinute: Long = 0L,
    val eveningMealRelation: String = MealRelation.AFTER_MEAL.name,
    val nightEnabled: Boolean = false,
    val nightHour: Long = 21L,
    val nightMinute: Long = 0L,
    val nightMealRelation: String = MealRelation.AFTER_MEAL.name,
    val startDateKey: String? = null,
    val endDateKey: String? = null,
    val recurrenceFrequency: String = RecurrenceFrequency.DAILY.name,
    val recurrenceInterval: Long = 1L,
    val recurrenceDays: String = "",
    val recurrenceEndType: String = RecurrenceEndType.NEVER.name,
    val recurrenceCount: Long? = null,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): Medicine = Medicine(
        id = id,
        memberId = memberId.ifBlank { FamilyMember.SELF_ID },
        name = name,
        dosage = dosage,
        form = form,
        mealRelation = MealRelation.fromRaw(mealRelation).name,
        notes = notes,
        morningEnabled = morningEnabled,
        morningHour = morningHour.toInt().coerceIn(0, 23),
        morningMinute = morningMinute.toInt().coerceIn(0, 59),
        morningMealRelation = MealRelation.fromRaw(morningMealRelation).name,
        afternoonEnabled = afternoonEnabled,
        afternoonHour = afternoonHour.toInt().coerceIn(0, 23),
        afternoonMinute = afternoonMinute.toInt().coerceIn(0, 59),
        afternoonMealRelation = MealRelation.fromRaw(afternoonMealRelation).name,
        eveningEnabled = eveningEnabled,
        eveningHour = eveningHour.toInt().coerceIn(0, 23),
        eveningMinute = eveningMinute.toInt().coerceIn(0, 59),
        eveningMealRelation = MealRelation.fromRaw(eveningMealRelation).name,
        nightEnabled = nightEnabled,
        nightHour = nightHour.toInt().coerceIn(0, 23),
        nightMinute = nightMinute.toInt().coerceIn(0, 59),
        nightMealRelation = MealRelation.fromRaw(nightMealRelation).name,
        startDateKey = startDateKey?.takeIf { it.isNotBlank() },
        endDateKey = endDateKey?.takeIf { it.isNotBlank() },
        recurrenceFrequency = RecurrenceFrequency.fromRaw(recurrenceFrequency).name,
        recurrenceInterval = recurrenceInterval.toInt().coerceIn(1, 365),
        recurrenceDays = recurrenceDays,
        recurrenceEndType = RecurrenceEndType.fromRaw(recurrenceEndType, endDateKey).name,
        recurrenceCount = recurrenceCount?.toInt()?.takeIf { it > 0 },
        active = active
    )
}

@Keep
@IgnoreExtraProperties
data class DoseLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val medicineId: String = "",
    val memberId: String = FamilyMember.SELF_ID,
    val memberName: String = "Self",
    val medicineName: String = "",
    val dosage: String = "",
    val dateKey: String = "",
    val slot: String = TimeSlot.MORNING.name,
    val status: String = DoseStatus.TAKEN.name,
    val scheduledTimeLabel: String = "",
    val takenAtEpochMillis: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): DoseLog = DoseLog(
        id = id,
        medicineId = medicineId,
        memberId = memberId.ifBlank { FamilyMember.SELF_ID },
        memberName = memberName.ifBlank { "Self" },
        medicineName = medicineName,
        dosage = dosage,
        dateKey = dateKey,
        slot = TimeSlot.fromRaw(slot).name,
        status = DoseStatus.fromRaw(status).name,
        scheduledTimeLabel = scheduledTimeLabel,
        takenAtEpochMillis = takenAtEpochMillis
    )
}

internal fun buildCreateUserProfileMap(
    userId: String,
    displayName: String,
    email: String,
    firstName: String = "",
    lastName: String = "",
    gender: String = "",
    dob: String = "",
    state: String = "",
    city: String = "",
    phone: String = "",
    morningHour: Int = 8,
    morningMinute: Int = 0,
    afternoonHour: Int = 13,
    afternoonMinute: Int = 30,
    eveningHour: Int = 18,
    eveningMinute: Int = 0,
    nightHour: Int = 21,
    nightMinute: Int = 0
): Map<String, Any> = mapOf(
    "userId" to userId,
    "displayName" to displayName,
    "email" to email,
    "firstName" to firstName.ifBlank { null },
    "lastName" to lastName.ifBlank { null },
    "gender" to gender.ifBlank { null },
    "dob" to dob.ifBlank { null },
    "state" to state.ifBlank { null },
    "city" to city.ifBlank { null },
    "phone" to phone.ifBlank { null },
    "morningHour" to morningHour.toLong(),
    "morningMinute" to morningMinute.toLong(),
    "afternoonHour" to afternoonHour.toLong(),
    "afternoonMinute" to afternoonMinute.toLong(),
    "eveningHour" to eveningHour.toLong(),
    "eveningMinute" to eveningMinute.toLong(),
    "nightHour" to nightHour.toLong(),
    "nightMinute" to nightMinute.toLong(),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterValues { it != null }.mapValues { it.value!! }
