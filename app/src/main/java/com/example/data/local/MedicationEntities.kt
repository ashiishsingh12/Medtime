package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.DoseLog
import com.example.model.FamilyMember
import com.example.model.Medicine
import com.example.model.RecurrenceEndType
import com.example.model.RecurrenceFrequency
import com.example.model.UserAccount
import com.example.model.UserProfile
import com.example.model.UserRole

@Entity(
    tableName = "user_accounts",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["phone"], unique = true)
    ]
)
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val dob: String,
    val state: String,
    val city: String,
    val email: String,
    val phone: String,
    val passwordSalt: String,
    val passwordHash: String,
    val role: String = UserRole.USER.name,
    val createdAtMillis: Long
) {
    fun toDomain(): UserAccount = UserAccount(
        id = id,
        firstName = firstName,
        lastName = lastName,
        gender = gender,
        dob = dob,
        state = state,
        city = city,
        email = email,
        phone = phone,
        role = UserRole.fromRaw(role),
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val relation: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
) {
    fun toDomain(): FamilyMember = FamilyMember(
        id = id,
        name = name,
        relation = relation,
        isDefault = isDefault
    )
}

@Entity(tableName = "patient_profile")
data class PatientProfileEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "Patient",
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val afternoonHour: Int = 13,
    val afternoonMinute: Int = 30,
    val eveningHour: Int = 18,
    val eveningMinute: Int = 0,
    val nightHour: Int = 21,
    val nightMinute: Int = 0
) {
    fun toDomain(): UserProfile = UserProfile(
        displayName = displayName,
        morningHour = morningHour,
        morningMinute = morningMinute,
        afternoonHour = afternoonHour,
        afternoonMinute = afternoonMinute,
        eveningHour = eveningHour,
        eveningMinute = eveningMinute,
        nightHour = nightHour,
        nightMinute = nightMinute
    )
}

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val memberId: String = FamilyMember.SELF_ID,
    val name: String,
    val dosage: String,
    val form: String,
    val mealRelation: String,
    val notes: String,
    val morningEnabled: Boolean,
    val morningHour: Int,
    val morningMinute: Int,
    val morningMealRelation: String = mealRelation,
    val afternoonEnabled: Boolean,
    val afternoonHour: Int,
    val afternoonMinute: Int,
    val afternoonMealRelation: String = mealRelation,
    val eveningEnabled: Boolean,
    val eveningHour: Int,
    val eveningMinute: Int,
    val eveningMealRelation: String = mealRelation,
    val nightEnabled: Boolean,
    val nightHour: Int,
    val nightMinute: Int,
    val nightMealRelation: String = mealRelation,
    val startDateKey: String? = null,
    val endDateKey: String? = null,
    val recurrenceFrequency: String = RecurrenceFrequency.DAILY.name,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: String = "",
    val recurrenceEndType: String = RecurrenceEndType.NEVER.name,
    val recurrenceCount: Int? = null,
    val active: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): Medicine = Medicine(
        id = id,
        memberId = memberId,
        name = name,
        dosage = dosage,
        form = form,
        mealRelation = mealRelation,
        notes = notes,
        morningEnabled = morningEnabled,
        morningHour = morningHour,
        morningMinute = morningMinute,
        morningMealRelation = morningMealRelation,
        afternoonEnabled = afternoonEnabled,
        afternoonHour = afternoonHour,
        afternoonMinute = afternoonMinute,
        afternoonMealRelation = afternoonMealRelation,
        eveningEnabled = eveningEnabled,
        eveningHour = eveningHour,
        eveningMinute = eveningMinute,
        eveningMealRelation = eveningMealRelation,
        nightEnabled = nightEnabled,
        nightHour = nightHour,
        nightMinute = nightMinute,
        nightMealRelation = nightMealRelation,
        startDateKey = startDateKey,
        endDateKey = endDateKey,
        recurrenceFrequency = recurrenceFrequency,
        recurrenceInterval = recurrenceInterval,
        recurrenceDays = recurrenceDays,
        recurrenceEndType = recurrenceEndType,
        recurrenceCount = recurrenceCount,
        active = active
    )
}

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey val id: String,
    val medicineId: String,
    val memberId: String = FamilyMember.SELF_ID,
    val memberName: String = "Self",
    val medicineName: String,
    val dosage: String,
    val dateKey: String,
    val slot: String,
    val status: String,
    val scheduledTimeLabel: String,
    val takenAtEpochMillis: Long
) {
    fun toDomain(): DoseLog = DoseLog(
        id = id,
        medicineId = medicineId,
        memberId = memberId,
        memberName = memberName,
        medicineName = medicineName,
        dosage = dosage,
        dateKey = dateKey,
        slot = slot,
        status = status,
        scheduledTimeLabel = scheduledTimeLabel,
        takenAtEpochMillis = takenAtEpochMillis
    )
}
