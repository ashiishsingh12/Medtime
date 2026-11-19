package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM patient_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<PatientProfileEntity?>

    @Query("SELECT * FROM patient_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): PatientProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PatientProfileEntity)

    @Query("SELECT * FROM medicines ORDER BY name COLLATE NOCASE ASC")
    fun observeMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :medicineId LIMIT 1")
    suspend fun getMedicineById(medicineId: String): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET active = :active, updatedAtMillis = :updatedAt WHERE id = :medicineId")
    suspend fun updateMedicineActive(medicineId: String, active: Boolean, updatedAt: Long)

    @Query("DELETE FROM medicines WHERE id = :medicineId")
    suspend fun deleteMedicine(medicineId: String)

    @Query("SELECT * FROM dose_logs ORDER BY takenAtEpochMillis DESC")
    fun observeDoseLogs(): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDoseLog(log: DoseLogEntity)

    @Query("DELETE FROM dose_logs WHERE id = :logId")
    suspend fun deleteDoseLog(logId: String)
}
