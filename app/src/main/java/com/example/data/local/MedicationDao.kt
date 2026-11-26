package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM family_members ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeFamilyMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    suspend fun getFamilyMembersOnce(): List<FamilyMemberEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFamilyMembersIfAbsent(members: List<FamilyMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFamilyMember(member: FamilyMemberEntity)

    @Query("UPDATE family_members SET name = :name WHERE id = :memberId")
    suspend fun updateFamilyMemberName(memberId: String, name: String)

    @Query("UPDATE dose_logs SET memberName = :memberName WHERE memberId = :memberId")
    suspend fun updateDoseLogMemberName(memberId: String, memberName: String)

    @Query("UPDATE patient_profile SET displayName = :displayName WHERE id = 1")
    suspend fun updateProfileDisplayName(displayName: String)

    @Query("DELETE FROM dose_logs WHERE memberId = :memberId OR medicineId IN (SELECT id FROM medicines WHERE memberId = :memberId)")
    suspend fun deleteDoseLogsByMember(memberId: String)

    @Query("DELETE FROM medicines WHERE memberId = :memberId")
    suspend fun deleteMedicinesByMember(memberId: String)

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun deleteFamilyMember(memberId: String)

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUserAccount(account: UserAccountEntity)

    @Query("SELECT * FROM user_accounts WHERE id = :userId LIMIT 1")
    suspend fun getUserAccountById(userId: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE lower(email) = lower(:email) LIMIT 1")
    suspend fun getUserAccountByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE phone = :phone LIMIT 1")
    suspend fun getUserAccountByPhone(phone: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun getLatestUserAccount(): UserAccountEntity?

    @Query("SELECT * FROM user_accounts ORDER BY CASE WHEN role = 'ADMIN' THEN 0 ELSE 1 END, createdAtMillis DESC")
    fun observeAllUserAccounts(): Flow<List<UserAccountEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_accounts WHERE role = 'ADMIN')")
    suspend fun hasAdminAccount(): Boolean

    @Query("UPDATE user_accounts SET role = :role WHERE id = :userId")
    suspend fun updateUserRole(userId: String, role: String)

    @Query("DELETE FROM user_accounts WHERE id = :userId")
    suspend fun deleteUserAccountById(userId: String)
}
