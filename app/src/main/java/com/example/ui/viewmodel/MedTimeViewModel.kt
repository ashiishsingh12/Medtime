package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MedicationRepository
import com.example.model.DayAdherenceSummary
import com.example.model.DoseLog
import com.example.model.DoseStatus
import com.example.model.Medicine
import com.example.model.MedicineInput
import com.example.model.ScheduledDoseItem
import com.example.model.TimeSlot
import com.example.model.UserProfile
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class TimetableDashboardState(
    val userProfile: UserProfile,
    val medicines: List<Medicine>,
    val doseLogs: List<DoseLog>,
    val todayDoses: List<ScheduledDoseItem>,
    val selectedFilter: TimeSlot?,
    val todayTakenCount: Int,
    val todayTotalCount: Int,
    val weeklySummary: List<DayAdherenceSummary>
)

class MedTimeViewModel(
    private val repository: MedicationRepository,
    private val onAlarmsSync: (List<Medicine>) -> Unit
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<TimeSlot?>(null)
    val selectedFilter: StateFlow<TimeSlot?> = _selectedFilter.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile()
        }
    }

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
        .onEach { meds -> onAlarmsSync(meds) }
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

    val dashboardState: StateFlow<UiState<TimetableDashboardState>> = combine(
        userProfileFlow,
        medicinesFlow,
        doseLogsFlow,
        _selectedFilter
    ) { profileState, medsState, logsState, filter ->
        when {
            medsState is UiState.Error -> UiState.Error(medsState.message)
            logsState is UiState.Error -> UiState.Error(logsState.message)
            profileState is UiState.Error -> UiState.Error(profileState.message)
            medsState is UiState.Success && logsState is UiState.Success && profileState is UiState.Success -> {
                val meds = medsState.data
                val logs = logsState.data
                val profile = profileState.data
                val todayKey = todayDateKey()
                val todayLogsByKey = logs
                    .filter { it.dateKey == todayKey }
                    .associateBy { "${it.slot}_${it.medicineId}" }

                val allTodayDoses = buildList {
                    meds.filter { it.active }.forEach { med ->
                        TimeSlot.entries.forEach { slot ->
                            if (med.isSlotEnabled(slot)) {
                                val log = todayLogsByKey["${slot.name}_${med.id}"]
                                add(
                                    ScheduledDoseItem(
                                        medicine = med,
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
                val logsByDate = logs.groupBy { it.dateKey }
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
                        userProfile = profile,
                        medicines = meds,
                        doseLogs = logs,
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

    fun setSlotFilter(slot: TimeSlot?) {
        _selectedFilter.value = slot
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
                    status = DoseStatus.TAKEN
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
                status = DoseStatus.SKIPPED
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
