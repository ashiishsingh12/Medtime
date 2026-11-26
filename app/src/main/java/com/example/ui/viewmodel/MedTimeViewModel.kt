package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MedicationRepository
import com.example.model.DayAdherenceSummary
import com.example.model.DoseLog
import com.example.model.DoseStatus
import com.example.model.FamilyMember
import com.example.model.Medicine
import com.example.model.MedicineInput
import com.example.model.RecurrenceRule
import com.example.model.ScheduledDoseItem
import com.example.model.TimeSlot
import com.example.model.TodayDoseFilter
import com.example.model.UserProfile
import com.example.model.formatRecurrenceSummary
import com.example.model.recentDateKeys
import com.example.model.todayDateKey
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class TimetableDashboardState(
    val userId: String,
    val userEmail: String,
    val userProfile: UserProfile,
    val familyMembers: List<FamilyMember>,
    val selectedMemberId: String,
    val selectedMember: FamilyMember?,
    val memberNamesById: Map<String, String>,
    val medicines: List<Medicine>,
    val doseLogs: List<DoseLog>,
    val todayDoses: List<ScheduledDoseItem>,
    val selectedFilter: TodayDoseFilter,
    val todayTakenCount: Int,
    val todayTotalCount: Int,
    val weeklySummary: List<DayAdherenceSummary>
)

class MedTimeViewModel(
    private val repository: MedicationRepository,
    private val currentUserId: String,
    private val currentUserEmail: String,
    private val currentUserDisplayName: String,
    private val onAlarmsSync: (List<Medicine>, Map<String, String>) -> Unit
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<TodayDoseFilter>(TodayDoseFilter.All)
    private val _selectedMemberId = MutableStateFlow(FamilyMember.SELF_ID)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val initResult = repository.ensureUserInitialized(
                userId = currentUserId,
                displayName = currentUserDisplayName,
                email = currentUserEmail
            )
            initResult.onFailure { err ->
                Log.w(TAG, "Failed to initialize user profile/members", err)
            }
        }
    }

    private fun unwrapFirestoreFailure(error: Throwable, fallbackMessage: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackMessage, rootError)
        return rootError.localizedMessage ?: rootError.message ?: fallbackMessage
    }

    private val familyMembersFlow: StateFlow<UiState<List<FamilyMember>>> =
        repository.observeFamilyMembers(currentUserId)
            .map<List<FamilyMember>, UiState<List<FamilyMember>>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(unwrapFirestoreFailure(error, "Failed to load family members")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val userProfileFlow: StateFlow<UiState<UserProfile>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile, UiState<UserProfile>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(unwrapFirestoreFailure(error, "Failed to load patient profile")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val medicinesFlow: StateFlow<UiState<List<Medicine>>> =
        repository.observeMedicines(currentUserId)
            .map<List<Medicine>, UiState<List<Medicine>>> { meds ->
                UiState.Success(meds.sortedBy { it.name.lowercase() })
            }
            .catch { error ->
                emit(UiState.Error(unwrapFirestoreFailure(error, "Failed to load medicines")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val doseLogsFlow: StateFlow<UiState<List<DoseLog>>> =
        repository.observeDoseLogs(currentUserId)
            .map<List<DoseLog>, UiState<List<DoseLog>>> { logs ->
                UiState.Success(logs.sortedByDescending { it.takenAtEpochMillis })
            }
            .catch { error ->
                emit(UiState.Error(unwrapFirestoreFailure(error, "Failed to load dose history")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val selectionFlow = combine(_selectedFilter, _selectedMemberId) { filter, memberId ->
        filter to memberId
    }

    val dashboardState: StateFlow<UiState<TimetableDashboardState>> = combine(
        userProfileFlow,
        familyMembersFlow,
        medicinesFlow,
        doseLogsFlow,
        selectionFlow
    ) { profileState, membersState, medsState, logsState, (filter, activeMemberId) ->
        when {
            medsState is UiState.Error -> UiState.Error(medsState.message)
            logsState is UiState.Error -> UiState.Error(logsState.message)
            profileState is UiState.Error -> UiState.Error(profileState.message)
            membersState is UiState.Error -> UiState.Error(membersState.message)
            medsState is UiState.Success &&
                logsState is UiState.Success &&
                profileState is UiState.Success &&
                membersState is UiState.Success -> {
                val allMeds = medsState.data
                val allLogs = logsState.data
                val profile = profileState.data
                val members = membersState.data
                val memberNamesById = members.associate { it.id to it.name }

                onAlarmsSync(allMeds, memberNamesById)

                val selectedMember = if (activeMemberId == FamilyMember.ALL_ID) {
                    null
                } else {
                    members.firstOrNull { it.id == activeMemberId }
                        ?: members.firstOrNull { it.id == FamilyMember.SELF_ID }
                        ?: members.firstOrNull()
                }
                val effectiveMemberId = if (activeMemberId == FamilyMember.ALL_ID) {
                    FamilyMember.ALL_ID
                } else {
                    selectedMember?.id ?: members.firstOrNull()?.id ?: FamilyMember.SELF_ID
                }

                val filteredMeds = if (effectiveMemberId == FamilyMember.ALL_ID) {
                    allMeds
                } else {
                    allMeds.filter { it.memberId == effectiveMemberId }
                }
                val filteredMedIds = filteredMeds.mapTo(HashSet()) { it.id }
                val filteredLogs = if (effectiveMemberId == FamilyMember.ALL_ID) {
                    allLogs
                } else {
                    allLogs.filter { it.memberId == effectiveMemberId || it.medicineId in filteredMedIds }
                }

                val todayKey = todayDateKey()
                val todayLogsByKey = filteredLogs
                    .filter { it.dateKey == todayKey }
                    .associateBy { "${it.slot}_${it.medicineId}" }

                val allTodayDoses = buildList {
                    filteredMeds
                        .filter { it.active && it.isScheduledOnDate(todayKey) }
                        .forEach { med ->
                            val memberName = memberNamesById[med.memberId] ?: "Self"
                            TimeSlot.entries.forEach { slot ->
                                if (med.isSlotEnabled(slot)) {
                                    val log = todayLogsByKey["${slot.name}_${med.id}"]
                                    add(
                                        ScheduledDoseItem(
                                            medicine = med,
                                            memberName = memberName,
                                            slot = slot,
                                            hour = med.hourFor(slot),
                                            minute = med.minuteFor(slot),
                                            doseLog = log
                                        )
                                    )
                                }
                            }
                        }
                }.sortedWith(
                    compareBy<ScheduledDoseItem>(
                        { it.slot.ordinal },
                        { it.hour },
                        { it.minute },
                        { it.medicine.name.lowercase() }
                    )
                )

                val totalScheduledPerDay = allTodayDoses.size
                val todayTaken = allTodayDoses.count { it.isTaken }
                val logsByDate = filteredLogs.groupBy { it.dateKey }
                val weekly = recentDateKeys(7).map { (dateKey, dayLabel) ->
                    val dayLogs = logsByDate[dateKey].orEmpty()
                    val taken = dayLogs.count { it.statusEnum() == DoseStatus.TAKEN }
                    DayAdherenceSummary(
                        dateKey = dateKey,
                        dayShortLabel = dayLabel,
                        takenCount = taken,
                        scheduledCount = maxOf(totalScheduledPerDay, taken)
                    )
                }

                UiState.Success(
                    TimetableDashboardState(
                        userId = currentUserId,
                        userEmail = currentUserEmail,
                        userProfile = profile,
                        familyMembers = members,
                        selectedMemberId = effectiveMemberId,
                        selectedMember = selectedMember,
                        memberNamesById = memberNamesById,
                        medicines = filteredMeds,
                        doseLogs = filteredLogs,
                        todayDoses = allTodayDoses,
                        selectedFilter = filter,
                        todayTakenCount = todayTaken,
                        todayTotalCount = totalScheduledPerDay,
                        weeklySummary = weekly
                    )
                )
            }
            else -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun selectMember(memberId: String) {
        _selectedMemberId.value = memberId
    }

    fun addFamilyMember(name: String, relation: String = "Family", onAdded: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.addFamilyMember(currentUserId, name, relation)
            _isSaving.value = false
            result.onSuccess { member ->
                _selectedMemberId.value = member.id
                _events.tryEmit("Switched to ${member.name}")
                onAdded()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to add family member.")
            }
        }
    }

    fun renameFamilyMember(memberId: String, newName: String, onRenamed: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.renameFamilyMember(currentUserId, memberId, newName)
            _isSaving.value = false
            result.onSuccess { updatedName ->
                _events.tryEmit("Updated profile name to $updatedName")
                onRenamed()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to rename profile.")
            }
        }
    }

    fun deleteFamilyMember(memberId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteFamilyMember(currentUserId, memberId)
            result.onSuccess { fallbackId ->
                if (_selectedMemberId.value == memberId) {
                    _selectedMemberId.value = fallbackId
                }
                _events.tryEmit("Removed member and their medicine schedule")
                onDeleted()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to remove member.")
            }
        }
    }

    fun setDoseFilter(filter: TodayDoseFilter) {
        _selectedFilter.value = filter
    }

    fun toggleDoseTaken(item: ScheduledDoseItem) {
        viewModelScope.launch {
            val todayKey = todayDateKey()
            val result = if (item.isTaken) {
                repository.clearDoseLog(
                    userId = currentUserId,
                    dateKey = todayKey,
                    slot = item.slot,
                    medicineId = item.medicine.id
                )
            } else {
                repository.markDoseStatus(
                    userId = currentUserId,
                    medicine = item.medicine,
                    slot = item.slot,
                    dateKey = todayKey,
                    status = DoseStatus.TAKEN,
                    memberName = item.memberName
                )
            }
            result.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not update dose status.")
            }
        }
    }

    fun markDoseSkipped(item: ScheduledDoseItem) {
        viewModelScope.launch {
            val result = repository.markDoseStatus(
                userId = currentUserId,
                medicine = item.medicine,
                slot = item.slot,
                dateKey = todayDateKey(),
                status = DoseStatus.SKIPPED,
                memberName = item.memberName
            )
            result.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not update dose status.")
            }
        }
    }

    fun saveMedicine(
        medicineId: String?,
        input: MedicineInput,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveMedicine(currentUserId, medicineId, input)
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit(
                    if (medicineId == null) "Saved ${input.name.trim()} & daily reminders scheduled"
                    else "Updated ${input.name.trim()} & reminder times"
                )
                onSaved()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save medicine.")
            }
        }
    }

    fun updateMedicineRecurrence(
        medicineId: String,
        rule: RecurrenceRule,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateMedicineRecurrence(currentUserId, medicineId, rule)
            _isSaving.value = false
            result.onSuccess {
                val summary = formatRecurrenceSummary(rule)
                _events.tryEmit("Schedule updated: $summary")
                onUpdated()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update repeat schedule.")
            }
        }
    }

    fun toggleMedicineActive(medicine: Medicine, active: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleMedicineActive(currentUserId, medicine.id, active)
            result.onSuccess {
                _events.tryEmit(
                    if (active) "${medicine.name} reminders enabled"
                    else "${medicine.name} reminders paused"
                )
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update reminder state.")
            }
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            val result = repository.deleteMedicine(currentUserId, medicine.id)
            result.onSuccess {
                _events.tryEmit("Removed ${medicine.name} from timetable")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to delete medicine.")
            }
        }
    }

    fun updateProfileAndDefaults(
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
        nightMinute: Int,
        onUpdated: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateProfileAndDefaultTimes(
                userId = currentUserId,
                displayName = displayName,
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                dob = dob,
                state = state,
                city = city,
                phone = phone,
                morningHour = morningHour,
                morningMinute = morningMinute,
                afternoonHour = afternoonHour,
                afternoonMinute = afternoonMinute,
                eveningHour = eveningHour,
                eveningMinute = eveningMinute,
                nightHour = nightHour,
                nightMinute = nightMinute
            )
            _isSaving.value = false
            result.onSuccess {
                _events.tryEmit("Patient settings saved")
                onUpdated()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save settings.")
            }
        }
    }

    private companion object {
        const val TAG = "MedTimeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
