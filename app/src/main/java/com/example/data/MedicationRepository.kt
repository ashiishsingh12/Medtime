package com.example.data

import android.content.Context
import android.util.Patterns
import com.example.data.auth.PasswordHasher
import com.example.data.local.DoseLogEntity
import com.example.data.local.FamilyMemberEntity
import com.example.data.local.MedTimeDatabase
import com.example.data.local.MedicationDao
import com.example.data.local.MedicineEntity
import com.example.data.local.PatientProfileEntity
import com.example.data.local.UserAccountEntity
import com.example.model.AdminIdentity
import com.example.model.DoseLog
import com.example.model.DoseStatus
import com.example.model.FamilyMember
import com.example.model.Medicine
import com.example.model.MedicineInput
import com.example.model.RecurrenceEndType
import com.example.model.RecurrenceFrequency
import com.example.model.RecurrenceRule
import com.example.model.RegistrationInput
import com.example.model.TimeSlot
import com.example.model.UserAccount
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.buildDoseLogId
import com.example.model.computeEffectiveEndDateKey
import com.example.model.formatTime12h
import com.example.model.isValidDmyDob
import com.example.model.normalizePhoneDigits
import com.example.model.toDaysCsv
import com.example.model.todayDateKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class MedicationRepository(private val dao: MedicationDao) {

    constructor(context: Context) : this(
        MedTimeDatabase.getInstance(context).medicationDao()
    )

    suspend fun hasAdminAccount(): Boolean = withContext(Dispatchers.IO) {
        dao.hasAdminAccount()
    }

    suspend fun claimAdminAccount(
        emailInput: String,
        phoneInput: String,
        password: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            if (dao.hasAdminAccount()) {
                return@withContext Result.failure(
                    IllegalStateException("Admin account is already configured. Please log in with your password.")
                )
            }
            val cleanEmail = emailInput.trim().lowercase().ifBlank { AdminIdentity.PRIMARY_ADMIN_EMAIL }
            val cleanPhone = normalizePhoneDigits(phoneInput).ifBlank { AdminIdentity.ADMIN_PHONE }

            require(AdminIdentity.isDesignatedAdminEmail(cleanEmail) && AdminIdentity.isDesignatedAdminPhone(cleanPhone)) {
                "Only the designated admin email and phone number can claim admin access."
            }
            require(password.length >= 6) {
                "Admin password must be at least 6 characters."
            }

            val existing = dao.getUserAccountByEmail(cleanEmail) ?: dao.getUserAccountByPhone(cleanPhone)
            if (existing != null) {
                return@withContext Result.failure(
                    IllegalStateException("Account already exists. Please log in with your existing password.")
                )
            }

            val hashed = PasswordHasher.hashPassword(password)
            val entity = UserAccountEntity(
                id = UUID.randomUUID().toString(),
                firstName = "Ashish",
                lastName = "Singh",
                gender = "Male",
                dob = "",
                state = "",
                city = "",
                email = cleanEmail,
                phone = cleanPhone,
                passwordSalt = hashed.saltBase64,
                passwordHash = hashed.hashWithAlg,
                role = UserRole.ADMIN.name,
                createdAtMillis = System.currentTimeMillis()
            )
            dao.insertUserAccount(entity)
            val domain = entity.toDomain()
            syncDefaultProfileNameFromUser(domain)
            Result.success(domain)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(input: RegistrationInput): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val firstName = input.firstName.trim().take(40)
            val lastName = input.lastName.trim().take(40)
            val gender = input.gender.trim().take(20)
            val dob = input.dob.trim()
            val state = input.state.trim().take(50)
            val city = input.city.trim().take(50)
            val email = input.email.trim().lowercase().take(100)
            val phone = normalizePhoneDigits(input.phone)
            val password = input.password

            require(firstName.isNotEmpty()) { "Please enter your first name." }
            require(lastName.isNotEmpty()) { "Please enter your last name." }
            require(gender.isNotEmpty()) { "Please select your gender." }
            require(isValidDmyDob(dob)) { "Please enter a valid date of birth (dd-mm-yyyy)." }
            require(state.isNotEmpty()) { "Please enter your state." }
            require(city.isNotEmpty()) { "Please enter your city." }
            require(email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                "Please enter a valid email address."
            }
            require(phone.length in 10..15) { "Please enter a valid 10-digit phone number." }
            require(password.length >= 6) { "Password must be at least 6 characters." }

            if (dao.getUserAccountByEmail(email) != null) {
                return@withContext Result.failure(IllegalArgumentException("An account with this email already exists."))
            }
            if (dao.getUserAccountByPhone(phone) != null) {
                return@withContext Result.failure(IllegalArgumentException("An account with this phone number already exists."))
            }

            val assignedRole = if (AdminIdentity.isDesignatedAdmin(email, phone)) {
                UserRole.ADMIN.name
            } else {
                UserRole.USER.name
            }

            val hashed = PasswordHasher.hashPassword(password)
            val entity = UserAccountEntity(
                id = UUID.randomUUID().toString(),
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                dob = dob,
                state = state,
                city = city,
                email = email,
                phone = phone,
                passwordSalt = hashed.saltBase64,
                passwordHash = hashed.hashWithAlg,
                role = assignedRole,
                createdAtMillis = System.currentTimeMillis()
            )
            dao.insertUserAccount(entity)
            val domain = entity.toDomain()
            syncDefaultProfileNameFromUser(domain)
            Result.success(domain)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(identifier: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        try {
            val cleanId = identifier.trim()
            require(cleanId.isNotEmpty()) { "Please enter your Email ID or Phone Number." }
            require(password.isNotEmpty()) { "Please enter your password." }

            var account = if (cleanId.contains("@")) {
                val lowered = cleanId.lowercase()
                dao.getUserAccountByEmail(lowered)
                    ?: when (lowered) {
                        AdminIdentity.PRIMARY_ADMIN_EMAIL -> dao.getUserAccountByEmail(AdminIdentity.ALT_ADMIN_EMAIL)
                        AdminIdentity.ALT_ADMIN_EMAIL -> dao.getUserAccountByEmail(AdminIdentity.PRIMARY_ADMIN_EMAIL)
                        else -> null
                    }
            } else {
                val normalizedPhone = normalizePhoneDigits(cleanId)
                (if (normalizedPhone.isNotEmpty()) dao.getUserAccountByPhone(normalizedPhone) else null)
                    ?: dao.getUserAccountByEmail(cleanId.lowercase())
            } ?: return@withContext Result.failure(IllegalArgumentException("Invalid Email/Phone or Password."))

            val verified = PasswordHasher.verifyPassword(
                password = password,
                saltBase64 = account.passwordSalt,
                storedHashWithAlg = account.passwordHash
            )
            if (!verified) {
                return@withContext Result.failure(IllegalArgumentException("Invalid Email/Phone or Password."))
            }

            if (AdminIdentity.isDesignatedAdmin(account.email, account.phone) &&
                account.role != UserRole.ADMIN.name
            ) {
                dao.updateUserRole(account.id, UserRole.ADMIN.name)
                account = account.copy(role = UserRole.ADMIN.name)
            }

            val domain = account.toDomain()
            syncDefaultProfileNameFromUser(domain)
            Result.success(domain)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserAccountById(userId: String): UserAccount? = withContext(Dispatchers.IO) {
        var account = dao.getUserAccountById(userId) ?: return@withContext null
        if (AdminIdentity.isDesignatedAdmin(account.email, account.phone) &&
            account.role != UserRole.ADMIN.name
        ) {
            dao.updateUserRole(account.id, UserRole.ADMIN.name)
            account = account.copy(role = UserRole.ADMIN.name)
        }
        account.toDomain()
    }

    fun observeAllUserAccounts(): Flow<List<UserAccount>> =
        dao.observeAllUserAccounts().map { list -> list.map { it.toDomain() } }

    suspend fun updateUserRole(
        actingUserId: String,
        targetUserId: String,
        newRole: UserRole
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val actingUser = dao.getUserAccountById(actingUserId)
                ?: return@withContext Result.failure(IllegalStateException("Signed-in user not found."))
            require(actingUser.role == UserRole.ADMIN.name) {
                "Admin access required."
            }
            val targetUser = dao.getUserAccountById(targetUserId)
                ?: return@withContext Result.failure(IllegalArgumentException("User account not found."))
            if (AdminIdentity.isDesignatedAdmin(targetUser.email, targetUser.phone) && newRole != UserRole.ADMIN) {
                return@withContext Result.failure(IllegalArgumentException("Primary admin role cannot be removed."))
            }
            dao.updateUserRole(targetUserId, newRole.name)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(
        actingUserId: String,
        targetUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val actingUser = dao.getUserAccountById(actingUserId)
                ?: return@withContext Result.failure(IllegalStateException("Signed-in user not found."))
            require(actingUser.role == UserRole.ADMIN.name) {
                "Admin access required."
            }
            require(actingUserId != targetUserId) {
                "You cannot delete your own active admin account."
            }
            val targetUser = dao.getUserAccountById(targetUserId)
                ?: return@withContext Result.failure(IllegalArgumentException("User account not found."))
            if (AdminIdentity.isDesignatedAdmin(targetUser.email, targetUser.phone)) {
                return@withContext Result.failure(IllegalArgumentException("Primary admin account cannot be deleted."))
            }
            dao.deleteUserAccountById(targetUserId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncDefaultProfileNameFromUser(user: UserAccount) {
        ensureDefaultFamilyMembers()
        ensureDefaultProfile()
        val firstName = user.firstName.trim().take(40)
        val fullName = user.fullName.trim().take(80)
        if (firstName.isBlank()) return

        val profile = dao.getProfileOnce()
        if (profile != null && profile.displayName.equals("Patient", ignoreCase = true)) {
            dao.updateProfileDisplayName(fullName.ifBlank { firstName })
        }
        val members = dao.getFamilyMembersOnce()
        val selfMember = members.firstOrNull { it.id == FamilyMember.SELF_ID }
        if (selfMember != null && selfMember.name.equals("Self", ignoreCase = true)) {
            dao.updateFamilyMemberName(FamilyMember.SELF_ID, firstName)
            dao.updateDoseLogMemberName(FamilyMember.SELF_ID, firstName)
        }
    }

    fun observeFamilyMembers(): Flow<List<FamilyMember>> =
        dao.observeFamilyMembers().map { list ->
            if (list.isEmpty()) FamilyMember.DefaultMembers
            else list.map { it.toDomain() }
        }

    suspend fun ensureDefaultFamilyMembers(): Result<Unit> = try {
        if (dao.getFamilyMembersOnce().isEmpty()) {
            val defaults = FamilyMember.DefaultMembers.mapIndexed { idx, member ->
                FamilyMemberEntity(
                    id = member.id,
                    name = member.name,
                    relation = member.relation,
                    sortOrder = idx,
                    isDefault = true
                )
            }
            dao.insertFamilyMembersIfAbsent(defaults)
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addFamilyMember(name: String, relation: String = "Family"): Result<FamilyMember> = try {
        val trimmedName = name.trim().take(40)
        require(trimmedName.isNotEmpty()) { "Please enter a member name." }
        val existingCount = dao.getFamilyMembersOnce().size
        val newMember = FamilyMember(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            relation = relation.trim().ifBlank { "Family" }.take(30),
            isDefault = false
        )
        dao.upsertFamilyMember(
            FamilyMemberEntity(
                id = newMember.id,
                name = newMember.name,
                relation = newMember.relation,
                sortOrder = existingCount + 1,
                isDefault = false
            )
        )
        Result.success(newMember)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun renameFamilyMember(memberId: String, newName: String): Result<String> = try {
        require(memberId.isNotBlank()) { "Member ID cannot be blank." }
        val trimmedName = newName.trim().take(40)
        require(trimmedName.isNotEmpty()) { "Please enter a name." }
        dao.updateFamilyMemberName(memberId, trimmedName)
        dao.updateDoseLogMemberName(memberId, trimmedName)
        if (memberId == FamilyMember.SELF_ID) {
            if (dao.getProfileOnce() == null) {
                dao.upsertProfile(PatientProfileEntity(displayName = trimmedName))
            } else {
                dao.updateProfileDisplayName(trimmedName)
            }
        }
        Result.success(trimmedName)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteFamilyMember(memberId: String): Result<String> = try {
        require(memberId.isNotBlank()) { "Member ID cannot be blank." }
        val remaining = dao.getFamilyMembersOnce().filter { it.id != memberId }
        require(remaining.isNotEmpty()) { "At least one profile must remain." }
        val fallbackId = remaining.first().id
        dao.deleteDoseLogsByMember(memberId)
        dao.deleteMedicinesByMember(memberId)
        dao.deleteFamilyMember(memberId)
        Result.success(fallbackId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

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
        val selfLabel = if (safeName.equals("Patient", ignoreCase = true)) {
            "Self"
        } else {
            safeName.take(40)
        }
        dao.updateFamilyMemberName(FamilyMember.SELF_ID, selfLabel)
        dao.updateDoseLogMemberName(FamilyMember.SELF_ID, selfLabel)
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

        val resolvedForm = input.customFormLabel.trim().take(40).ifBlank { input.form.name }
        val resolvedMemberId = input.memberId.trim().ifBlank { FamilyMember.SELF_ID }
        val resolvedFrequency = input.recurrenceFrequency
        val resolvedInterval = input.recurrenceInterval.coerceIn(1, 365)
        val resolvedDaysCsv = if (resolvedFrequency == RecurrenceFrequency.WEEKLY) {
            toDaysCsv(input.recurrenceDaysOfWeek)
        } else {
            ""
        }
        val resolvedEndType = if (resolvedFrequency == RecurrenceFrequency.NONE) {
            RecurrenceEndType.ON_DATE
        } else {
            input.recurrenceEndType
        }
        val resolvedCount = if (resolvedEndType == RecurrenceEndType.AFTER_OCCURRENCES) {
            (input.recurrenceCount ?: 1).coerceIn(1, 3650)
        } else {
            null
        }
        val needsStartAnchor = resolvedFrequency != RecurrenceFrequency.DAILY ||
            resolvedInterval > 1 ||
            resolvedEndType != RecurrenceEndType.NEVER ||
            !input.startDateKey.isNullOrBlank()
        val resolvedStartDate = input.startDateKey?.trim()?.takeIf { it.isNotBlank() }
            ?: if (needsStartAnchor) todayDateKey() else null
        val explicitEndDate = when {
            resolvedFrequency == RecurrenceFrequency.NONE -> resolvedStartDate
            resolvedEndType == RecurrenceEndType.ON_DATE ->
                input.endDateKey?.trim()?.takeIf { it.isNotBlank() }
            resolvedEndType == RecurrenceEndType.AFTER_OCCURRENCES ->
                computeEffectiveEndDateKey(
                    RecurrenceRule(
                        frequency = resolvedFrequency,
                        interval = resolvedInterval,
                        daysOfWeek = input.recurrenceDaysOfWeek,
                        endType = resolvedEndType,
                        startDateKey = resolvedStartDate,
                        endDateKey = null,
                        occurrenceCount = resolvedCount
                    )
                )
            else -> null
        }
        if (resolvedStartDate != null && explicitEndDate != null) {
            require(explicitEndDate >= resolvedStartDate) {
                "End date cannot be earlier than start date."
            }
        }

        dao.upsertMedicine(
            MedicineEntity(
                id = targetId,
                memberId = resolvedMemberId,
                name = trimmedName,
                dosage = trimmedDosage,
                form = resolvedForm,
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
                startDateKey = resolvedStartDate,
                endDateKey = explicitEndDate,
                recurrenceFrequency = resolvedFrequency.name,
                recurrenceInterval = resolvedInterval,
                recurrenceDays = resolvedDaysCsv,
                recurrenceEndType = resolvedEndType.name,
                recurrenceCount = resolvedCount,
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

    suspend fun updateMedicineRecurrence(
        medicineId: String,
        rule: RecurrenceRule
    ): Result<Unit> = try {
        require(medicineId.isNotBlank()) { "Medicine ID cannot be blank." }
        val existing = dao.getMedicineById(medicineId)
            ?: throw IllegalArgumentException("Medicine not found.")
        val resolvedInterval = rule.interval.coerceIn(1, 365)
        val resolvedDaysCsv = if (rule.frequency == RecurrenceFrequency.WEEKLY) {
            toDaysCsv(rule.daysOfWeek)
        } else {
            ""
        }
        val resolvedEndType = if (rule.frequency == RecurrenceFrequency.NONE) {
            RecurrenceEndType.ON_DATE
        } else {
            rule.endType
        }
        val resolvedCount = if (resolvedEndType == RecurrenceEndType.AFTER_OCCURRENCES) {
            (rule.occurrenceCount ?: 1).coerceIn(1, 3650)
        } else {
            null
        }
        val resolvedStart = rule.startDateKey?.trim()?.takeIf { it.isNotBlank() } ?: todayDateKey()
        val resolvedEnd = when {
            rule.frequency == RecurrenceFrequency.NONE -> resolvedStart
            resolvedEndType == RecurrenceEndType.ON_DATE ->
                rule.endDateKey?.trim()?.takeIf { it.isNotBlank() }
            resolvedEndType == RecurrenceEndType.AFTER_OCCURRENCES ->
                computeEffectiveEndDateKey(
                    rule.copy(
                        interval = resolvedInterval,
                        endType = resolvedEndType,
                        startDateKey = resolvedStart,
                        occurrenceCount = resolvedCount
                    )
                )
            else -> null
        }
        if (resolvedEnd != null) {
            require(resolvedEnd >= resolvedStart) { "End date cannot be earlier than start date." }
        }
        dao.upsertMedicine(
            existing.copy(
                startDateKey = resolvedStart,
                endDateKey = resolvedEnd,
                recurrenceFrequency = rule.frequency.name,
                recurrenceInterval = resolvedInterval,
                recurrenceDays = resolvedDaysCsv,
                recurrenceEndType = resolvedEndType.name,
                recurrenceCount = resolvedCount,
                active = true,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        Result.success(Unit)
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
        status: DoseStatus,
        memberName: String? = null
    ): Result<Unit> = try {
        require(medicine.id.isNotBlank()) { "Medicine ID cannot be blank." }
        val logId = buildDoseLogId(dateKey, slot, medicine.id)
        val timeLabel = formatTime12h(medicine.hourFor(slot), medicine.minuteFor(slot))
        val resolvedMemberName = memberName?.trim()?.take(40)?.ifBlank { null }
            ?: dao.getFamilyMembersOnce().firstOrNull { it.id == medicine.memberId }?.name
            ?: "Self"
        dao.upsertDoseLog(
            DoseLogEntity(
                id = logId,
                medicineId = medicine.id,
                memberId = medicine.memberId,
                memberName = resolvedMemberName,
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
