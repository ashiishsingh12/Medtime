package com.example.data

import android.content.Context
import com.example.data.local.DoseLogEntity
import com.example.data.local.MedTimeDatabase
import com.example.data.local.MedicationDao
import com.example.data.local.MedicineEntity
import com.example.data.local.PatientProfileEntity
import com.example.model.DoseLog
import com.example.model.DoseStatus
import com.example.model.Medicine
import com.example.model.MedicineInput
import com.example.model.TimeSlot
import com.example.model.UserProfile
import com.example.model.buildDoseLogId
import com.example.model.formatTime12h
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MedicationRepository(private val dao: MedicationDao) {

    constructor(context: Context) : this(
        MedTimeDatabase.getInstance(context).medicationDao()
    )

    fun observeUserProfile(): Flow<UserProfile> =
        dao.observeProfile().map { it?.toDomain() ?: UserProfile() }

    suspend fun ensureDefaultProfile(): Result<Unit> = try {
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(PatientProfileEntity())
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateProfileAndDefaultTimes(
        displayName: String,
        morningHour: Int,
        morningMinute: Int,
        afternoonHour: Int,
        afternoonMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
        nightHour: Int,
        nightMinute: Int
    ): Result<Unit> = try {
        require(morningHour in 0..23 && morningMinute in 0..59) { "Invalid morning time" }
        require(afternoonHour in 0..23 && afternoonMinute in 0..59) { "Invalid afternoon time" }
        require(eveningHour in 0..23 && eveningMinute in 0..59) { "Invalid evening time" }
        require(nightHour in 0..23 && nightMinute in 0..59) { "Invalid night time" }
        val safeName = displayName.trim().ifBlank { "Patient" }.take(80)
        dao.upsertProfile(
            PatientProfileEntity(
                id = 1,
                displayName = safeName,
                morningHour = morningHour,
                morningMinute = morningMinute,
                afternoonHour = afternoonHour,
                afternoonMinute = afternoonMinute,
                eveningHour = eveningHour,
                eveningMinute = eveningMinute,
                nightHour = nightHour,
                nightMinute = nightMinute
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeMedicines(): Flow<List<Medicine>> =
        dao.observeMedicines().map { list -> list.map { it.toDomain() } }

    suspend fun saveMedicine(
        medicineId: String?,
        input: MedicineInput
    ): Result<String> = try {
        val trimmedName = input.name.trim()
        val trimmedDosage = input.dosage.trim()
        val trimmedNotes = input.notes.trim().take(200)
        require(trimmedName.isNotEmpty() && trimmedName.length <= 80) {
            "Medicine name must be between 1 and 80 characters."
        }
        require(trimmedDosage.isNotEmpty() && trimmedDosage.length <= 40) {
            "Dosage must be between 1 and 40 characters."
        }
        require(
            input.morningEnabled || input.afternoonEnabled ||
                input.eveningEnabled || input.nightEnabled
        ) {
            "Select at least one time slot (Morning, Afternoon, Evening, or Night)."
        }

        val now = System.currentTimeMillis()
        val targetId = if (medicineId.isNullOrBlank()) UUID.randomUUID().toString() else medicineId
        val existing = if (!medicineId.isNullOrBlank()) dao.getMedicineById(medicineId) else null

        val fallbackMeal = when {
            input.morningEnabled -> input.morningMealRelation
            input.afternoonEnabled -> input.afternoonMealRelation
            input.eveningEnabled -> input.eveningMealRelation
            else -> input.nightMealRelation
        }

        dao.upsertMedicine(
            MedicineEntity(
                id = targetId,
                name = trimmedName,
                dosage = trimmedDosage,
                form = input.form.name,
                mealRelation = fallbackMeal.name,
                notes = trimmedNotes,
                morningEnabled = input.morningEnabled,
                morningHour = input.morningHour.coerceIn(0, 23),
                morningMinute = input.morningMinute.coerceIn(0, 59),
                morningMealRelation = input.morningMealRelation.name,
                afternoonEnabled = input.afternoonEnabled,
                afternoonHour = input.afternoonHour.coerceIn(0, 23),
                afternoonMinute = input.afternoonMinute.coerceIn(0, 59),
                afternoonMealRelation = input.afternoonMealRelation.name,
                eveningEnabled = input.eveningEnabled,
                eveningHour = input.eveningHour.coerceIn(0, 23),
                eveningMinute = input.eveningMinute.coerceIn(0, 59),
                eveningMealRelation = input.eveningMealRelation.name,
                nightEnabled = input.nightEnabled,
                nightHour = input.nightHour.coerceIn(0, 23),
                nightMinute = input.nightMinute.coerceIn(0, 59),
                nightMealRelation = input.nightMealRelation.name,
                active = input.active,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
        )
        Result.success(targetId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleMedicineActive(medicineId: String, active: Boolean): Result<Unit> = try {
        require(medicineId.isNotBlank()) { "Medicine ID cannot be blank." }
        dao.updateMedicineActive(medicineId, active, System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteMedicine(medicineId: String): Result<Unit> = try {
        require(medicineId.isNotBlank()) { "Medicine ID cannot be blank." }
        dao.deleteMedicine(medicineId)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeDoseLogs(): Flow<List<DoseLog>> =
        dao.observeDoseLogs().map { list -> list.map { it.toDomain() } }

    suspend fun markDoseStatus(
        medicine: Medicine,
        slot: TimeSlot,
        dateKey: String,
        status: DoseStatus
    ): Result<Unit> = try {
        require(medicine.id.isNotBlank()) { "Medicine ID cannot be blank." }
        val logId = buildDoseLogId(dateKey, slot, medicine.id)
        val timeLabel = formatTime12h(medicine.hourFor(slot), medicine.minuteFor(slot))
        dao.upsertDoseLog(
            DoseLogEntity(
                id = logId,
                medicineId = medicine.id,
                medicineName = medicine.name.trim().ifBlank { "Medicine" }.take(80),
                dosage = medicine.dosage.trim().ifBlank { "1 dose" }.take(40),
                dateKey = dateKey,
                slot = slot.name,
                status = status.name,
                scheduledTimeLabel = timeLabel,
                takenAtEpochMillis = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun clearDoseLog(
        dateKey: String,
        slot: TimeSlot,
        medicineId: String
    ): Result<Unit> = try {
        val logId = buildDoseLogId(dateKey, slot, medicineId)
        dao.deleteDoseLog(logId)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
