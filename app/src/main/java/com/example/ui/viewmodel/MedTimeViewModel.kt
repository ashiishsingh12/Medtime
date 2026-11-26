package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MedicationRepository
import com.example.data.auth.AuthSessionManager
import com.example.model.DayAdherenceSummary
import com.example.model.DoseLog
import com.example.model.DoseStatus
import com.example.model.FamilyMember
import com.example.model.Medicine
import com.example.model.MedicineInput
import com.example.model.RegistrationInput
import com.example.model.ScheduledDoseItem
import com.example.model.TimeSlot
import com.example.model.TodayDoseFilter
import com.example.model.UserAccount
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.recentDateKeys
import com.example.model.todayDateKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

sealed interface AuthUiState {
    data object Checking : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Authenticated(
        val user: UserAccount,
        val sessionExpiresAtMillis: Long
    ) : AuthUiState
}

data class TimetableDashboardState(
    val loggedInUser: UserAccount?,
    val sessionExpiresAtMillis: Long,
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
    private val sessionManager: AuthSessionManager,
    private val onAlarmsSync: (List<Medicine>, Map<String, String>) -> Unit
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _hasAdminAccount = MutableStateFlow(false)
    val hasAdminAccount: StateFlow<Boolean> = _hasAdminAccount.asStateFlow()

    private val _selectedFilter = MutableStateFlow<TodayDoseFilter>(TodayDoseFilter.All)
    val selectedFilter: StateFlow<TodayDoseFilter> = _selectedFilter.asStateFlow()

    private val _selectedMemberId = MutableStateFlow(FamilyMember.SELF_ID)
    val selectedMemberId: StateFlow<String> = _selectedMemberId.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val adminUsers: StateFlow<List<UserAccount>> = combine(
        repository.observeAllUserAccounts(),
        _authState
    ) { accounts, auth ->
        val currentUser = (auth as? AuthUiState.Authenticated)?.user
        if (currentUser?.isAdmin == true) accounts else emptyList()
    }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing user accounts", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.ensureDefaultFamilyMembers()
            repository.ensureDefaultProfile()
            _hasAdminAccount.value = repository.hasAdminAccount()
            val session = sessionManager.getValidSession()
            if (session != null) {
                val account = repository.getUserAccountById(session.userId)
                if (account != null) {
                    _authState.value = AuthUiState.Authenticated(account, session.expiresAtMillis)
                } else {
                    sessionManager.clearSession()
                    _authState.value = AuthUiState.Unauthenticated
                }
            } else {
                _authState.value = AuthUiState.Unauthenticated
            }
            _hasAdminAccount.value = repository.hasAdminAccount()
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val result = repository.loginUser(identifier, password)
            _isAuthBusy.value = false
            result.onSuccess { account ->
                val expiresAt = sessionManager.saveSession(account.id)
                _hasAdminAccount.value = repository.hasAdminAccount()
                _authState.value = AuthUiState.Authenticated(account, expiresAt)
            }.onFailure { err ->
                _authError.value = err.localizedMessage ?: "Sign in failed."
            }
        }
    }

    fun register(input: RegistrationInput) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val result = repository.registerUser(input)
            _isAuthBusy.value = false
            result.onSuccess { account ->
                val expiresAt = sessionManager.saveSession(account.id)
                _hasAdminAccount.value = repository.hasAdminAccount()
                _authState.value = AuthUiState.Authenticated(account, expiresAt)
                _events.tryEmit(
                    if (account.isAdmin) "Welcome Admin, ${account.firstName}!"
                    else "Welcome, ${account.firstName}!"
                )
            }.onFailure { err ->
                _authError.value = err.localizedMessage ?: "Registration failed."
            }
        }
    }

    fun claimAdminAccount(email: String, phone: String, password: String) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val result = repository.claimAdminAccount(email, phone, password)
            _isAuthBusy.value = false
            result.onSuccess { account ->
                val expiresAt = sessionManager.saveSession(account.id)
                _hasAdminAccount.value = true
                _authState.value = AuthUiState.Authenticated(account, expiresAt)
                _events.tryEmit("Admin account activated for ${account.email}")
            }.onFailure { err ->
                _hasAdminAccount.value = repository.hasAdminAccount()
                _authError.value = err.localizedMessage ?: "Admin setup failed."
            }
        }
    }

    fun updateUserRole(targetUserId: String, newRole: UserRole) {
        val currentUser = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        if (!currentUser.isAdmin) {
            _events.tryEmit("Admin access required.")
            return
        }
        viewModelScope.launch {
            val result = repository.updateUserRole(currentUser.id, targetUserId, newRole)
            result.onSuccess {
                _events.tryEmit("Updated user role to ${newRole.displayName}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not update user role.")
            }
        }
    }

    fun deleteUserAccount(targetUserId: String) {
        val currentUser = (_authState.value as? AuthUiState.Authenticated)?.user ?: return
        if (!currentUser.isAdmin) {
            _events.tryEmit("Admin access required.")
            return
        }
        viewModelScope.launch {
            val result = repository.deleteUserAccount(currentUser.id, targetUserId)
            result.onSuccess {
                _events.tryEmit("User account removed")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not remove user.")
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _authError.value = null
        _authState.value = AuthUiState.Unauthenticated
    }

    private val familyMembersFlow: StateFlow<UiState<List<FamilyMember>>> = repository.observeFamilyMembers()
        .map<List<FamilyMember>, UiState<List<FamilyMember>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing family members", error)
            emit(UiState.Success(FamilyMember.DefaultMembers))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(FamilyMember.DefaultMembers)
        )

    private val userProfileFlow: StateFlow<UiState<UserProfile>> = repository.observeUserProfile()
        .map<UserProfile, UiState<UserProfile>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load patient profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val medicinesFlow: StateFlow<UiState<List<Medicine>>> = repository.observeMedicines()
        .map<List<Medicine>, UiState<List<Medicine>>> { meds ->
            UiState.Success(meds.sortedBy { it.name.lowercase() })
        }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing medicines", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load medicines"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val doseLogsFlow: StateFlow<UiState<List<DoseLog>>> = repository.observeDoseLogs()
        .map<List<DoseLog>, UiState<List<DoseLog>>> { logs ->
            UiState.Success(logs.sortedByDescending { it.takenAtEpochMillis })
        }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing dose logs", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load dose history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val selectionAndAuthFlow = combine(_selectedFilter, _selectedMemberId, _authState) { filter, memberId, auth ->
        Triple(filter, memberId, auth)
    }

    val dashboardState: StateFlow<UiState<TimetableDashboardState>> = combine(
        userProfileFlow,
        familyMembersFlow,
        medicinesFlow,
        doseLogsFlow,
        selectionAndAuthFlow
    ) { profileState, membersState, medsState, logsState, (filter, activeMemberId, auth) ->
        when {
            medsState is UiState.Error -> UiState.Error(medsState.message)
            logsState is UiState.Error -> UiState.Error(logsState.message)
            profileState is UiState.Error -> UiState.Error(profileState.message)
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

                val authUser = (auth as? AuthUiState.Authenticated)?.user
                val expiresAt = (auth as? AuthUiState.Authenticated)?.sessionExpiresAtMillis ?: 0L

                UiState.Success(
                    TimetableDashboardState(
                        loggedInUser = authUser,
                        sessionExpiresAtMillis = expiresAt,
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
            val result = repository.addFamilyMember(name, relation)
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
            val result = repository.renameFamilyMember(memberId, newName)
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
            val result = repository.deleteFamilyMember(memberId)
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
                    dateKey = todayKey,
                    slot = item.slot,
                    medicineId = item.medicine.id
                )
            } else {
                repository.markDoseStatus(
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
            val result = repository.saveMedicine(medicineId, input)
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
        rule: com.example.model.RecurrenceRule,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateMedicineRecurrence(medicineId, rule)
            _isSaving.value = false
            result.onSuccess {
                val summary = com.example.model.formatRecurrenceSummary(rule)
                _events.tryEmit("Schedule updated: $summary")
                onUpdated()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update repeat schedule.")
            }
        }
    }

    fun toggleMedicineActive(medicine: Medicine, active: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleMedicineActive(medicine.id, active)
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
            val result = repository.deleteMedicine(medicine.id)
            result.onSuccess {
                _events.tryEmit("Removed ${medicine.name} from timetable")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to delete medicine.")
            }
        }
    }

    fun updateProfileAndDefaults(
        displayName: String,
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
