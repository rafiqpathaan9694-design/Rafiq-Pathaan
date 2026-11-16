package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupExportManager
import com.example.backup.BackupInspectResult
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsState
import com.example.data.local.DocumentStorageManager
import com.example.data.local.SettingsPreferences
import com.example.data.model.DocumentSlot
import com.example.data.model.FormStatus
import com.example.data.model.StudentRecord
import com.example.data.repository.StudentRepository
import com.example.domain.FormSection
import com.example.domain.FormValidation
import com.example.domain.ValidationResult
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class StudentFilter { ALL, PENDING, COMPLETED }

data class DashboardStats(
    val totalStudents: Int = 0,
    val savedForms: Int = 0,
    val pendingForms: Int = 0,
    val completedForms: Int = 0
)

data class FormUiState(
    val record: StudentRecord = StudentRecord(),
    val validation: ValidationResult = ValidationResult(),
    val activeSection: FormSection = FormSection.PERSONAL,
    val showValidationErrors: Boolean = false,
    val isSaving: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.studentDao()
    val documentStorage = DocumentStorageManager(application)
    val repository = StudentRepository(dao, documentStorage)
    private val prefs = SettingsPreferences(application)

    val settingsState: StateFlow<AppSettingsState> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettingsState()
    )

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StudentFilter.ALL)
    val statusFilter: StateFlow<StudentFilter> = _statusFilter.asStateFlow()

    val allStudents: StateFlow<List<StudentRecord>> = repository.allStudents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dashboardStats: StateFlow<DashboardStats> = allStudents.combine(MutableStateFlow(Unit)) { list, _ ->
        val pending = list.count { it.status == FormStatus.PENDING }
        val completed = list.count { it.status == FormStatus.COMPLETED }
        DashboardStats(
            totalStudents = list.size,
            savedForms = list.size,
            pendingForms = pending,
            completedForms = completed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    val filteredStudents: StateFlow<List<StudentRecord>> = combine(
        allStudents,
        _searchQuery,
        _statusFilter
    ) { list, query, filter ->
        val q = query.trim().lowercase(Locale.ENGLISH)
        list.filter { student ->
            val matchesFilter = when (filter) {
                StudentFilter.ALL -> true
                StudentFilter.PENDING -> student.status == FormStatus.PENDING
                StudentFilter.COMPLETED -> student.status == FormStatus.COMPLETED
            }
            val matchesQuery = q.isEmpty() ||
                student.studentName.lowercase(Locale.ENGLISH).contains(q) ||
                student.studentMobile.contains(q) ||
                student.emergencyMobile.contains(q) ||
                student.aadhaarNumber.contains(q) ||
                student.formNumber.lowercase(Locale.ENGLISH).contains(q)
            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _formState = MutableStateFlow(FormUiState())
    val formState: StateFlow<FormUiState> = _formState.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = prefs.settingsFlow.first()
            _isUnlocked.value = !initial.pinEnabled
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val ok = prefs.verifyPin(pin, settingsState.value)
        if (ok) _isUnlocked.value = true
        return ok
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { prefs.setLanguage(language) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            prefs.setPin(pin)
            _isUnlocked.value = true
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            prefs.disablePin()
            _isUnlocked.value = true
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: StudentFilter) {
        _statusFilter.value = filter
    }

    fun startNewForm() {
        _formState.value = FormUiState(
            record = StudentRecord(),
            validation = ValidationResult(),
            activeSection = FormSection.PERSONAL,
            showValidationErrors = false
        )
    }

    fun loadStudentForEdit(studentId: Long) {
        viewModelScope.launch {
            val loaded = repository.getStudentById(studentId) ?: StudentRecord()
            _formState.value = FormUiState(
                record = loaded,
                validation = ValidationResult(),
                activeSection = FormSection.PERSONAL,
                showValidationErrors = false
            )
        }
    }

    fun setActiveSection(section: FormSection) {
        _formState.update { it.copy(activeSection = section) }
    }

    fun updateDraft(transform: (StudentRecord) -> StudentRecord) {
        _formState.update { current ->
            val raw = transform(current.record)
            val normalized = raw.copy(
                studentName = raw.studentName.uppercase(Locale.ENGLISH)
            )
            val updatedValidation = if (current.showValidationErrors) {
                FormValidation.validateForCompletion(normalized)
            } else {
                current.validation
            }
            current.copy(record = normalized, validation = updatedValidation)
        }
    }

    fun attachDocumentFromUri(slot: DocumentSlot, uri: Uri, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val savedPath = documentStorage.saveImageFromUri(uri, slot)
            if (savedPath != null) {
                val oldPath = _formState.value.record.documentPathFor(slot)
                if (oldPath.isNotBlank() && oldPath != savedPath) {
                    documentStorage.deleteFileIfExists(oldPath)
                }
                updateDraft { it.withDocumentPath(slot, savedPath) }
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun attachDocumentFromCameraFile(slot: DocumentSlot, file: File, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val savedPath = documentStorage.saveImageFromFile(file, slot)
            if (savedPath != null) {
                val oldPath = _formState.value.record.documentPathFor(slot)
                if (oldPath.isNotBlank() && oldPath != savedPath) {
                    documentStorage.deleteFileIfExists(oldPath)
                }
                updateDraft { it.withDocumentPath(slot, savedPath) }
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun removeDocument(slot: DocumentSlot) {
        val oldPath = _formState.value.record.documentPathFor(slot)
        if (oldPath.isNotBlank()) {
            documentStorage.deleteFileIfExists(oldPath)
        }
        updateDraft { it.withDocumentPath(slot, "") }
    }

    fun saveAsPendingDraft(onSuccess: (Long) -> Unit, onValidationError: () -> Unit) {
        val current = _formState.value.record
        val draftValidation = FormValidation.validateDraft(current)
        if (!draftValidation.isValid) {
            _formState.update {
                it.copy(
                    validation = draftValidation,
                    showValidationErrors = true,
                    activeSection = FormSection.PERSONAL
                )
            }
            onValidationError()
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            val toSave = current.copy(status = FormStatus.PENDING)
            val savedId = repository.saveStudent(toSave)
            val refreshed = repository.getStudentById(savedId) ?: toSave.copy(id = savedId)
            _formState.update { it.copy(record = refreshed, isSaving = false) }
            onSuccess(savedId)
        }
    }

    fun validateAndProceedToReview(): Boolean {
        val current = _formState.value.record
        val validation = FormValidation.validateForCompletion(current)
        if (validation.isValid) {
            _formState.update { it.copy(validation = validation, showValidationErrors = false) }
            return true
        }
        val firstErrorSection = FormSection.entries.firstOrNull {
            (validation.missingBySection[it] ?: 0) > 0
        } ?: FormSection.PERSONAL
        _formState.update {
            it.copy(
                validation = validation,
                showValidationErrors = true,
                activeSection = firstErrorSection
            )
        }
        return false
    }

    fun confirmAndSaveCompleted(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            val toSave = _formState.value.record.copy(status = FormStatus.COMPLETED)
            val savedId = repository.saveStudent(toSave)
            val refreshed = repository.getStudentById(savedId) ?: toSave.copy(id = savedId)
            _formState.update { it.copy(record = refreshed, isSaving = false) }
            onSuccess(savedId)
        }
    }

    fun deleteStudent(record: StudentRecord) {
        viewModelScope.launch {
            repository.deleteStudent(record)
        }
    }

    suspend fun getStudentForPreview(studentId: Long): StudentRecord? {
        return repository.getStudentById(studentId)
    }

    fun exportCsvToUri(uri: Uri, onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val students = repository.getAllStudentsOnce()
                val out = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Cannot write to selected file")
                out.use { BackupExportManager.exportCsvToStream(students, it) }
                onDone(Result.success(students.size))
            } catch (e: Exception) {
                onDone(Result.failure(e))
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun exportBackupZipToUri(uri: Uri, onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            try {
                val students = repository.getAllStudentsOnce()
                val out = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Cannot write backup file")
                out.use { BackupExportManager.exportBackupZip(students, it) }
                onDone(Result.success(students.size))
            } catch (e: Exception) {
                onDone(Result.failure(e))
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun inspectBackupFile(uri: Uri, onDone: (Result<BackupInspectResult>) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            val result = BackupExportManager.inspectBackupZip(getApplication(), uri)
            _backupBusy.value = false
            onDone(result)
        }
    }

    fun restoreBackupFile(uri: Uri, replaceExisting: Boolean, onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _backupBusy.value = true
            val result = BackupExportManager.restoreBackupZip(
                context = getApplication(),
                uri = uri,
                replaceExisting = replaceExisting,
                dao = dao
            )
            _backupBusy.value = false
            onDone(result)
        }
    }
}
