package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentStorageManager
import com.example.data.model.DocumentSlot
import com.example.data.model.StudentRecord
import com.example.domain.FormSection
import com.example.i18n.AppStrings
import com.example.i18n.LocalAppStrings
import com.example.ui.FormUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormScreen(
    formState: FormUiState,
    documentStorage: DocumentStorageManager,
    onUpdateRecord: ((StudentRecord) -> StudentRecord) -> Unit,
    onSelectSection: (FormSection) -> Unit,
    onAttachUri: (DocumentSlot, android.net.Uri) -> Unit,
    onAttachCameraFile: (DocumentSlot, File) -> Unit,
    onRemoveDoc: (DocumentSlot) -> Unit,
    onSaveDraft: () -> Unit,
    onReviewAndFinalize: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Pending slot for gallery / camera launchers
    var activeUploadSlot by remember { mutableStateOf<DocumentSlot?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val slot = activeUploadSlot
        if (uri != null && slot != null) {
            onAttachUri(slot, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val slot = activeUploadSlot
        val file = pendingCameraFile
        if (success && slot != null && file != null) {
            onAttachCameraFile(slot, file)
        } else {
            file?.delete()
        }
    }

    BackHandler {
        showDiscardDialog = true
    }

    val isEditing = formState.record.id != 0L

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) strings.editFormTitle else strings.createFormTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveDraft,
                        enabled = !formState.isSaving,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_draft")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.saveDraftBtn, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onReviewAndFinalize,
                        enabled = !formState.isSaving,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("btn_review_finalize")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.reviewAndSaveBtn, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Section Navigation Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FormSection.entries) { section ->
                    val errCount = if (formState.showValidationErrors) {
                        formState.validation.missingBySection[section] ?: 0
                    } else 0
                    val selected = formState.activeSection == section
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSection(section) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sectionShortLabel(section, strings))
                                if (errCount > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(errCount.toString())
                                    }
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("tab_section_${section.name.lowercase()}")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.requiredNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (formState.showValidationErrors && !formState.validation.isValid) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.validationBannerTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.missingFieldsCount.format(formState.validation.totalErrors),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                when (formState.activeSection) {
                    FormSection.PERSONAL -> PersonalDetailsSection(
                        record = formState.record,
                        validation = formState.validation,
                        onUpdate = onUpdateRecord
                    )
                    FormSection.TENTH -> TenthDetailsSection(
                        record = formState.record,
                        validation = formState.validation,
                        onUpdate = onUpdateRecord
                    )
                    FormSection.TWELFTH -> TwelfthDetailsSection(
                        record = formState.record,
                        validation = formState.validation,
                        onUpdate = onUpdateRecord
                    )
                    FormSection.EXAM_CITY -> ExamCitiesSection(
                        record = formState.record,
                        validation = formState.validation,
                        onUpdate = onUpdateRecord
                    )
                    FormSection.PARENTS -> ParentsDetailsSection(
                        record = formState.record,
                        validation = formState.validation,
                        onUpdate = onUpdateRecord
                    )
                    FormSection.ADDRESS -> AddressDetailsSection(
                        record = formState.record,
                        validation = formState.validation,
                        onUpdate = onUpdateRecord
                    )
                    FormSection.DOCUMENTS -> DocumentsSection(
                        record = formState.record,
                        validation = formState.validation,
                        onPickGallery = { slot ->
                            activeUploadSlot = slot
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onTakeCamera = { slot ->
                            try {
                                val (uri, file) = documentStorage.createTempCameraUri()
                                activeUploadSlot = slot
                                pendingCameraFile = file
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Camera unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRemove = onRemoveDoc
                    )
                }

                // Previous / Next Section quick switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val idx = formState.activeSection.index
                    if (idx > 0) {
                        OutlinedButton(
                            onClick = { onSelectSection(FormSection.entries[idx - 1]) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(sectionShortLabel(FormSection.entries[idx - 1], strings))
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (idx < FormSection.entries.lastIndex) {
                        Button(
                            onClick = { onSelectSection(FormSection.entries[idx + 1]) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(sectionShortLabel(FormSection.entries[idx + 1], strings))
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(strings.editFormTitle) },
            text = { Text("Save as pending draft or exit without saving unsaved changes?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onSaveDraft()
                    }
                ) {
                    Text(strings.saveDraftBtn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(strings.cancelBtn)
                    }
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onBack()
                        }
                    ) {
                        Text("Exit", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

private fun sectionShortLabel(section: FormSection, strings: AppStrings): String = when (section) {
    FormSection.PERSONAL -> strings.sectionA
    FormSection.TENTH -> strings.sectionB
    FormSection.TWELFTH -> strings.sectionC
    FormSection.EXAM_CITY -> strings.sectionD
    FormSection.PARENTS -> strings.sectionE
    FormSection.ADDRESS -> strings.sectionF
    FormSection.DOCUMENTS -> strings.sectionG
}
