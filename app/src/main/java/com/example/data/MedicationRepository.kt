package com.example.data

import android.content.Context
import com.example.R
import com.example.model.DoseLog
import com.example.model.DoseStatus
import com.example.model.FamilyMember
import com.example.model.Medicine
import com.example.model.MedicineInput
import com.example.model.RecurrenceEndType
import com.example.model.RecurrenceFrequency
import com.example.model.RecurrenceRule
import com.example.model.TimeSlot
import com.example.model.UserProfile
import com.example.model.buildDoseLogId
import com.example.model.computeEffectiveEndDateKey
import com.example.model.formatTime12h
import com.example.model.normalizePhoneDigits
import com.example.model.toDaysCsv
import com.example.model.todayDateKey
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MedicationRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun requireMatchingUserId(userId: String): String {
        val currentUid = requireUserId()
        require(userId.isNotBlank()) { "User ID cannot be blank." }
        check(currentUid == userId) { "Signed-in user changed; refusing cross-user access." }
        return currentUid
    }

    private fun userDoc(uid: String): DocumentReference =
        db.collection("users").document(uid)

    private fun familyMembersCol(uid: String) =
        userDoc(uid).collection("familyMembers")

    private fun medicinesCol(uid: String) =
        userDoc(uid).collection("medicines")

    private fun doseLogsCol(uid: String) =
        userDoc(uid).collection("doseLogs")

    suspend fun ensureUserInitialized(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireMatchingUserId(userId)
            val safeDisplayName = displayName.trim().ifBlank {
                email.substringBefore("@").trim().ifBlank { "Patient" }
            }.take(80)
            val safeEmail = email.trim().take(120)
            val firstName = safeDisplayName.substringBefore(" ").trim()
                .takeIf { it.isNotBlank() && !it.equals("Patient", ignoreCase = true) }
                ?.take(40)
                ?: ""

            val profileSnapshot = userDoc(uid).get().await()
            if (!profileSnapshot.exists()) {
                val payload = buildCreateUserProfileMap(
                    userId = uid,
                    displayName = safeDisplayName,
                    email = safeEmail,
                    firstName = firstName
                )
                userDoc(uid).set(payload).await()
            }

            val existingMembers = familyMembersCol(uid).limit(1).get().await()
            if (existingMembers.isEmpty) {
                val selfLabel = firstName.ifBlank { "Self" }
                val batch = db.batch()
                FamilyMember.DefaultMembers.forEachIndexed { idx, member ->
                    val resolvedName = if (member.id == FamilyMember.SELF_ID) selfLabel else member.name
                    val memberPayload = mapOf(
                        "userId" to uid,
                        "name" to resolvedName.take(40),
                        "relation" to member.relation.take(30),
                        "sortOrder" to idx.toLong(),
                        "defaultMember" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(familyMembersCol(uid).document(member.id), memberPayload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfileDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.toDomain() ?: UserProfile()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeFamilyMembers(userId: String): Flow<List<FamilyMember>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/familyMembers"
        emitAll(
            familyMembersCol(userId)
                .orderBy("sortOrder")
                .snapshots()
                .map { snapshot ->
                    val items = snapshot.toObjects(
                        FamilyMemberDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                    if (items.isEmpty()) FamilyMember.DefaultMembers else items
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMedicines(userId: String): Flow<List<Medicine>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/medicines"
        emitAll(
            medicinesCol(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MedicineDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDoseLogs(userId: String): Flow<List<DoseLog>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/doseLogs"
        emitAll(
            doseLogsCol(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DoseLogDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addFamilyMember(
        userId: String,
        name: String,
        relation: String = "Family"
    ): Result<FamilyMember> {
        var docPath = "users/$userId/familyMembers"
        return try {
            val uid = requireMatchingUserId(userId)
            val trimmedName = name.trim().take(40)
            val safeRelation = relation.trim().ifBlank { "Family" }.take(30)
            require(trimmedName.isNotEmpty()) { "Please enter a member name." }

            val existingCount = familyMembersCol(uid).get().await().size()
            val memberId = UUID.randomUUID().toString()
            val docRef = familyMembersCol(uid).document(memberId)
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "name" to trimmedName,
                "relation" to safeRelation,
                "sortOrder" to (existingCount + 1).toLong().coerceIn(0L, 10000L),
                "defaultMember" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                FamilyMember(
                    id = memberId,
                    name = trimmedName,
                    relation = safeRelation,
                    isDefault = false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun renameFamilyMember(
        userId: String,
        memberId: String,
        newName: String
    ): Result<String> {
        var docPath = "users/$userId/familyMembers/$memberId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(memberId.isNotBlank()) { "Member ID cannot be blank." }
            val trimmedName = newName.trim().take(40)
            require(trimmedName.isNotEmpty()) { "Please enter a name." }

            val memberRef = familyMembersCol(uid).document(memberId)
            docPath = memberRef.path
            val memberSnap = memberRef.get().await()
            if (memberSnap.exists()) {
                memberRef.update(
                    mapOf(
                        "name" to trimmedName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                memberRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to trimmedName,
                        "relation" to if (memberId == FamilyMember.SELF_ID) "Self" else "Family",
                        "sortOrder" to 0L,
                        "defaultMember" to (memberId in setOf(FamilyMember.SELF_ID, FamilyMember.MOM_ID, FamilyMember.DAD_ID)),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (memberId == FamilyMember.SELF_ID) {
                val userSnapshot = userDoc(uid).get().await()
                if (userSnapshot.exists()) {
                    userDoc(uid).update(
                        mapOf(
                            "displayName" to trimmedName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val memberLogs = doseLogsCol(uid).whereEqualTo("memberId", memberId).get().await()
            if (!memberLogs.isEmpty) {
                memberLogs.documents.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { logDoc ->
                        batch.update(
                            logDoc.reference,
                            mapOf(
                                "memberName" to trimmedName,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                }
            }
            Result.success(trimmedName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFamilyMember(
        userId: String,
        memberId: String
    ): Result<String> {
        var docPath = "users/$userId/familyMembers/$memberId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(memberId.isNotBlank()) { "Member ID cannot be blank." }
            val allMembers = familyMembersCol(uid).orderBy("sortOrder").get().await().documents
            val remaining = allMembers.filter { it.id != memberId }
            require(remaining.isNotEmpty()) { "At least one profile must remain." }
            val fallbackId = remaining.first().id

            val logsToDelete = doseLogsCol(uid).whereEqualTo("memberId", memberId).get().await().documents
            val medsToDelete = medicinesCol(uid).whereEqualTo("memberId", memberId).get().await().documents
            val refsToDelete = buildList {
                logsToDelete.forEach { add(it.reference) }
                medsToDelete.forEach { add(it.reference) }
                add(familyMembersCol(uid).document(memberId))
            }
            docPath = familyMembersCol(uid).document(memberId).path

            refsToDelete.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                batch.commit().await()
            }
            Result.success(fallbackId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileAndDefaultTimes(
        userId: String,
        displayName: String,
        firstName: String,
        lastName: String,
        gender: String,
        dob: String,
        state: String,
        city: String,
        phone: String,
        morningHour: Int,
        morningMinute: Int,
        afternoonHour: Int,
        afternoonMinute: Int,
        eveningHour: Int,
        eveningMinute: Int,
        nightHour: Int,
        nightMinute: Int
    ): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(morningHour in 0..23 && morningMinute in 0..59) { "Invalid morning time" }
            require(afternoonHour in 0..23 && afternoonMinute in 0..59) { "Invalid afternoon time" }
            require(eveningHour in 0..23 && eveningMinute in 0..59) { "Invalid evening time" }
            require(nightHour in 0..23 && nightMinute in 0..59) { "Invalid night time" }

            val cleanFirst = firstName.trim().take(40)
            val cleanLast = lastName.trim().take(40)
            val cleanGender = gender.trim().take(20)
            val cleanDob = dob.trim().take(20)
            val cleanState = state.trim().take(50)
            val cleanCity = city.trim().take(50)
            val cleanPhone = normalizePhoneDigits(phone).take(20)
            val combinedName = listOf(cleanFirst, cleanLast).filter { it.isNotEmpty() }.joinToString(" ")
            val safeName = displayName.trim().ifBlank { combinedName }.ifBlank { "Patient" }.take(80)
            val currentEmail = auth.currentUser?.email.orEmpty().trim().take(120)

            val ref = userDoc(uid)
            docPath = ref.path
            val existing = ref.get().await()
            if (existing.exists()) {
                val updates = mapOf<String, Any>(
                    "displayName" to safeName,
                    "email" to currentEmail,
                    "firstName" to (cleanFirst.ifBlank { FieldValue.delete() }),
                    "lastName" to (cleanLast.ifBlank { FieldValue.delete() }),
                    "gender" to (cleanGender.ifBlank { FieldValue.delete() }),
                    "dob" to (cleanDob.ifBlank { FieldValue.delete() }),
                    "state" to (cleanState.ifBlank { FieldValue.delete() }),
                    "city" to (cleanCity.ifBlank { FieldValue.delete() }),
                    "phone" to (cleanPhone.ifBlank { FieldValue.delete() }),
                    "morningHour" to morningHour.toLong(),
                    "morningMinute" to morningMinute.toLong(),
                    "afternoonHour" to afternoonHour.toLong(),
                    "afternoonMinute" to afternoonMinute.toLong(),
                    "eveningHour" to eveningHour.toLong(),
                    "eveningMinute" to eveningMinute.toLong(),
                    "nightHour" to nightHour.toLong(),
                    "nightMinute" to nightMinute.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.update(updates).await()
            } else {
                val createPayload = buildCreateUserProfileMap(
                    userId = uid,
                    displayName = safeName,
                    email = currentEmail,
                    firstName = cleanFirst,
                    lastName = cleanLast,
                    gender = cleanGender,
                    dob = cleanDob,
                    state = cleanState,
                    city = cleanCity,
                    phone = cleanPhone,
                    morningHour = morningHour,
                    morningMinute = morningMinute,
                    afternoonHour = afternoonHour,
                    afternoonMinute = afternoonMinute,
                    eveningHour = eveningHour,
                    eveningMinute = eveningMinute,
                    nightHour = nightHour,
                    nightMinute = nightMinute
                )
                ref.set(createPayload).await()
            }

            val selfLabel = when {
                cleanFirst.isNotBlank() -> cleanFirst.take(40)
                safeName.equals("Patient", ignoreCase = true) -> "Self"
                else -> safeName.take(40)
            }
            val selfRef = familyMembersCol(uid).document(FamilyMember.SELF_ID)
            if (selfRef.get().await().exists()) {
                selfRef.update(
                    mapOf(
                        "name" to selfLabel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveMedicine(
        userId: String,
        medicineId: String?,
        input: MedicineInput
    ): Result<String> {
        var docPath = "users/$userId/medicines"
        return try {
            val uid = requireMatchingUserId(userId)
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
                toDaysCsv(input.recurrenceDaysOfWeek).take(30)
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

            val isCreate = medicineId.isNullOrBlank()
            val docRef = if (isCreate) {
                medicinesCol(uid).document()
            } else {
                medicinesCol(uid).document(medicineId!!)
            }
            docPath = docRef.path

            val baseFields = mutableMapOf<String, Any?>(
                "memberId" to resolvedMemberId,
                "name" to trimmedName,
                "dosage" to trimmedDosage,
                "form" to resolvedForm,
                "mealRelation" to fallbackMeal.name,
                "notes" to trimmedNotes,
                "morningEnabled" to input.morningEnabled,
                "morningHour" to input.morningHour.coerceIn(0, 23).toLong(),
                "morningMinute" to input.morningMinute.coerceIn(0, 59).toLong(),
                "morningMealRelation" to input.morningMealRelation.name,
                "afternoonEnabled" to input.afternoonEnabled,
                "afternoonHour" to input.afternoonHour.coerceIn(0, 23).toLong(),
                "afternoonMinute" to input.afternoonMinute.coerceIn(0, 59).toLong(),
                "afternoonMealRelation" to input.afternoonMealRelation.name,
                "eveningEnabled" to input.eveningEnabled,
                "eveningHour" to input.eveningHour.coerceIn(0, 23).toLong(),
                "eveningMinute" to input.eveningMinute.coerceIn(0, 59).toLong(),
                "eveningMealRelation" to input.eveningMealRelation.name,
                "nightEnabled" to input.nightEnabled,
                "nightHour" to input.nightHour.coerceIn(0, 23).toLong(),
                "nightMinute" to input.nightMinute.coerceIn(0, 59).toLong(),
                "nightMealRelation" to input.nightMealRelation.name,
                "recurrenceFrequency" to resolvedFrequency.name,
                "recurrenceInterval" to resolvedInterval.toLong(),
                "recurrenceDays" to resolvedDaysCsv,
                "recurrenceEndType" to resolvedEndType.name,
                "active" to input.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (isCreate) {
                baseFields["userId"] = uid
                baseFields["startDateKey"] = resolvedStartDate
                baseFields["endDateKey"] = explicitEndDate
                baseFields["recurrenceCount"] = resolvedCount?.toLong()
                baseFields["createdAt"] = FieldValue.serverTimestamp()
                val createPayload = baseFields.filterValues { it != null }.mapValues { it.value!! }
                docRef.set(createPayload).await()
            } else {
                baseFields["startDateKey"] = resolvedStartDate ?: FieldValue.delete()
                baseFields["endDateKey"] = explicitEndDate ?: FieldValue.delete()
                baseFields["recurrenceCount"] = resolvedCount?.toLong() ?: FieldValue.delete()
                val updatePayload = baseFields.mapValues { it.value!! }
                docRef.update(updatePayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (medicineId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun updateMedicineRecurrence(
        userId: String,
        medicineId: String,
        rule: RecurrenceRule
    ): Result<Unit> {
        var docPath = "users/$userId/medicines/$medicineId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(medicineId.isNotBlank()) { "Medicine ID cannot be blank." }
            val docRef = medicinesCol(uid).document(medicineId)
            docPath = docRef.path

            val resolvedInterval = rule.interval.coerceIn(1, 365)
            val resolvedDaysCsv = if (rule.frequency == RecurrenceFrequency.WEEKLY) {
                toDaysCsv(rule.daysOfWeek).take(30)
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

            val updatePayload = mapOf<String, Any>(
                "startDateKey" to resolvedStart,
                "endDateKey" to (resolvedEnd ?: FieldValue.delete()),
                "recurrenceFrequency" to rule.frequency.name,
                "recurrenceInterval" to resolvedInterval.toLong(),
                "recurrenceDays" to resolvedDaysCsv,
                "recurrenceEndType" to resolvedEndType.name,
                "recurrenceCount" to (resolvedCount?.toLong() ?: FieldValue.delete()),
                "active" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMedicineActive(
        userId: String,
        medicineId: String,
        active: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/medicines/$medicineId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(medicineId.isNotBlank()) { "Medicine ID cannot be blank." }
            val docRef = medicinesCol(uid).document(medicineId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMedicine(
        userId: String,
        medicineId: String
    ): Result<Unit> {
        var docPath = "users/$userId/medicines/$medicineId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(medicineId.isNotBlank()) { "Medicine ID cannot be blank." }
            val docRef = medicinesCol(uid).document(medicineId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markDoseStatus(
        userId: String = auth.currentUser?.uid.orEmpty(),
        medicine: Medicine,
        slot: TimeSlot,
        dateKey: String,
        status: DoseStatus,
        memberName: String? = null
    ): Result<Unit> {
        var docPath = "users/$userId/doseLogs"
        return try {
            val uid = if (userId.isBlank()) requireUserId() else requireMatchingUserId(userId)
            require(medicine.id.isNotBlank()) { "Medicine ID cannot be blank." }
            val logId = buildDoseLogId(dateKey, slot, medicine.id)
            val docRef = doseLogsCol(uid).document(logId)
            docPath = docRef.path

            val timeLabel = formatTime12h(medicine.hourFor(slot), medicine.minuteFor(slot)).take(20)
            val resolvedMemberId = medicine.memberId.trim().ifBlank { FamilyMember.SELF_ID }
            val resolvedMemberName = memberName?.trim()?.take(40)?.ifBlank { null }
                ?: familyMembersCol(uid).document(resolvedMemberId).get().await()
                    .getString("name")?.takeIf { it.isNotBlank() }?.take(40)
                ?: "Self"
            val safeMedName = medicine.name.trim().ifBlank { "Medicine" }.take(80)
            val safeDosage = medicine.dosage.trim().ifBlank { "1 dose" }.take(40)
            val takenEpoch = System.currentTimeMillis()

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "memberName" to resolvedMemberName,
                        "medicineName" to safeMedName,
                        "dosage" to safeDosage,
                        "status" to status.name,
                        "scheduledTimeLabel" to timeLabel,
                        "takenAtEpochMillis" to takenEpoch,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "medicineId" to medicine.id,
                        "memberId" to resolvedMemberId,
                        "memberName" to resolvedMemberName,
                        "medicineName" to safeMedName,
                        "dosage" to safeDosage,
                        "dateKey" to dateKey,
                        "slot" to slot.name,
                        "status" to status.name,
                        "scheduledTimeLabel" to timeLabel,
                        "takenAtEpochMillis" to takenEpoch,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearDoseLog(
        userId: String,
        dateKey: String,
        slot: TimeSlot,
        medicineId: String
    ): Result<Unit> {
        var docPath = "users/$userId/doseLogs"
        return try {
            val uid = requireMatchingUserId(userId)
            val logId = buildDoseLogId(dateKey, slot, medicineId)
            val docRef = doseLogsCol(uid).document(logId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
