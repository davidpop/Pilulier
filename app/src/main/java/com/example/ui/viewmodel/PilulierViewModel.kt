package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PilulierApplication
import com.example.data.model.DoseItem
import com.example.data.model.DoseSchedule
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import com.example.data.model.MedicationWithSchedules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ScreenTab {
    HOME,           // Aujourd'hui
    TREATMENTS,     // Mes traitements
    HISTORY,        // Historique
    SETTINGS,       // Paramètres
    ADD_EDIT        // Formulaire d'ajout / modification
}

enum class TreatmentFilter(val label: String) {
    ALL("Tous"),
    ACTIVE("En cours"),
    UPCOMING("À venir"),
    PAUSED("Suspendus"),
    FINISHED("Terminés")
}

class PilulierViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PilulierApplication
    private val repository = app.repository
    private val preferencesRepository = app.preferencesRepository

    // Navigation d'écran
    private val _currentScreen = MutableStateFlow(ScreenTab.HOME)
    val currentScreen: StateFlow<ScreenTab> = _currentScreen.asStateFlow()

    private val _previousScreen = MutableStateFlow(ScreenTab.HOME)

    // Médicament en cours d'édition (null si ajout d'un nouveau)
    private val _editingMedication = MutableStateFlow<MedicationWithSchedules?>(null)
    val editingMedication: StateFlow<MedicationWithSchedules?> = _editingMedication.asStateFlow()

    // Filtre des traitements
    private val _treatmentFilter = MutableStateFlow(TreatmentFilter.ALL)
    val treatmentFilter: StateFlow<TreatmentFilter> = _treatmentFilter.asStateFlow()

    // Traitements complets
    val allTreatments: StateFlow<List<MedicationWithSchedules>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Date sélectionnée pour l'historique
    private val _historyDate = MutableStateFlow(LocalDate.now())
    val historyDate: StateFlow<LocalDate> = _historyDate.asStateFlow()

    // Doses pour aujourd'hui
    val todayDate: LocalDate = LocalDate.now()
    val todayDoses: StateFlow<List<DoseItem>> = repository.getDosesForDate(todayDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Doses pour la date d'historique sélectionnée
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historyDoses: StateFlow<List<DoseItem>> = _historyDate.flatMapLatest { date ->
        repository.getDosesForDate(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Préférences utilisateur
    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val snoozeMinutes: StateFlow<Int> = preferencesRepository.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val appTheme: StateFlow<String> = preferencesRepository.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    // Navigation
    fun navigateTo(screen: ScreenTab) {
        if (_currentScreen.value != screen && _currentScreen.value != ScreenTab.ADD_EDIT) {
            _previousScreen.value = _currentScreen.value
        }
        _currentScreen.value = screen
    }

    fun navigateBack() {
        _currentScreen.value = _previousScreen.value
        _editingMedication.value = null
    }

    fun openAddMedication() {
        _editingMedication.value = null
        _previousScreen.value = _currentScreen.value
        _currentScreen.value = ScreenTab.ADD_EDIT
    }

    fun openEditMedication(item: MedicationWithSchedules) {
        _editingMedication.value = item
        _previousScreen.value = _currentScreen.value
        _currentScreen.value = ScreenTab.ADD_EDIT
    }

    fun setTreatmentFilter(filter: TreatmentFilter) {
        _treatmentFilter.value = filter
    }

    fun setHistoryDate(date: LocalDate) {
        _historyDate.value = date
    }

    fun previousHistoryDay() {
        _historyDate.value = _historyDate.value.minusDays(1)
    }

    fun nextHistoryDay() {
        _historyDate.value = _historyDate.value.plusDays(1)
    }

    fun resetHistoryToToday() {
        _historyDate.value = LocalDate.now()
    }

    // Actions sur les prises
    fun markDoseTaken(doseItem: DoseItem) {
        viewModelScope.launch {
            repository.markDoseTaken(
                medicationId = doseItem.medicationId,
                scheduledDateTime = doseItem.scheduledDateTime,
                medicationName = doseItem.medicationName,
                dosage = doseItem.dosage
            )
        }
    }

    fun unmarkDoseTaken(doseItem: DoseItem) {
        viewModelScope.launch {
            repository.unmarkDoseTaken(
                medicationId = doseItem.medicationId,
                scheduledDateTime = doseItem.scheduledDateTime
            )
        }
    }

    fun snoozeDose(doseItem: DoseItem, minutes: Int? = null) {
        viewModelScope.launch {
            val delay = minutes ?: snoozeMinutes.value
            repository.snoozeDose(
                medicationId = doseItem.medicationId,
                scheduledDateTime = doseItem.scheduledDateTime,
                medicationName = doseItem.medicationName,
                dosage = doseItem.dosage,
                snoozeMinutes = delay
            )
        }
    }

    // Gestion des traitements
    fun saveTreatment(
        id: Long = 0,
        name: String,
        dosage: String,
        form: String,
        quantityPerDose: String,
        instructions: String,
        startDate: LocalDate,
        endDate: LocalDate,
        schedules: List<Pair<Int, Int>> // List of (Hour, Minute)
    ) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = endDate.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

            val medication = Medication(
                id = id,
                name = name.trim(),
                dosage = dosage.trim(),
                form = form.trim(),
                quantityPerDose = quantityPerDose.trim(),
                instructions = instructions.trim(),
                startDate = startMillis,
                endDate = endMillis,
                isPaused = _editingMedication.value?.medication?.isPaused ?: false,
                updatedAt = System.currentTimeMillis()
            )

            val doseSchedules = schedules.map { (hour, minute) ->
                DoseSchedule(
                    medicationId = id,
                    hour = hour,
                    minute = minute
                )
            }

            repository.saveMedication(medication, doseSchedules)
            _editingMedication.value = null
            _currentScreen.value = ScreenTab.TREATMENTS
        }
    }

    fun toggleMedicationPaused(medicationId: Long, isPaused: Boolean) {
        viewModelScope.launch {
            repository.setMedicationPaused(medicationId, isPaused)
        }
    }

    fun deleteMedication(item: MedicationWithSchedules, deleteHistory: Boolean = false) {
        viewModelScope.launch {
            repository.deleteMedication(item, deleteHistory)
        }
    }

    // Gestion des préférences
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setSnoozeMinutes(minutes)
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setAppTheme(theme)
        }
    }
}
