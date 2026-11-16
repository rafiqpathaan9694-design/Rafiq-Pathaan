package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.StudentRecord
import com.example.i18n.LocalAppStrings
import com.example.ui.components.SectionCard
import com.example.ui.theme.SuccessGreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormReviewScreen(
    record: StudentRecord,
    isSaving: Boolean,
    onBackToEdit: () -> Unit,
    onConfirmSave: (onSavedId: (Long) -> Unit) -> Unit,
    onOpenPrintPreview: (Long) -> Unit,
    onGoDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var savedStudentId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.finalConfirmationTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToEdit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backToEdit)
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
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToEdit,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_review_edit")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.backToEdit)
                    }
                    Button(
                        onClick = {
                            onConfirmSave { newId -> savedStudentId = newId }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_confirm_save")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.confirmAndSave, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = strings.finalConfirmationSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            item {
                SectionCard(title = strings.sectionA) {
                    ReviewRow("Student Name", record.studentName)
                    ReviewRow("Date of Birth", record.dateOfBirth)
                    ReviewRow("Gender", record.gender)
                    ReviewRow("Father's Name", record.fatherName)
                    ReviewRow("Mother's Name", record.motherName)
                    ReviewRow("Aadhaar Number", record.aadhaarNumber)
                    ReviewRow("Student Mobile", record.studentMobile)
                    ReviewRow("Student Email", record.studentEmail)
                    ReviewRow("Emergency Mobile", record.emergencyMobile)
                    ReviewRow("Emergency Email", record.emergencyEmail.ifBlank { "-" })
                    ReviewRow("Category", record.category)
                    ReviewRow("Religion", record.religion)
                    ReviewRow("PwD", record.personWithDisability)
                    ReviewRow("Category Certificate", record.categoryCertificate.ifBlank { "-" })
                    ReviewRow("Certificate ID", record.categoryCertificateId.ifBlank { "-" })
                    ReviewRow("Issuing Authority", record.issuingAuthorityName.ifBlank { "-" })
                    ReviewRow("Issue Date", record.certificateIssueDate.ifBlank { "-" })
                    ReviewRow("Domicile State", record.domicileState)
                    ReviewRow("Annual Income", record.familyAnnualIncome)
                    ReviewRow("Paper Medium", record.questionPaperMedium)
                    ReviewRow("Only Girl Child", record.onlyGirlChild)
                }
            }

            item {
                SectionCard(title = strings.sectionB) {
                    ReviewRow("Passing Year", record.tenthPassingYear)
                    ReviewRow("Roll Number", record.tenthRollNumber)
                    ReviewRow("Percentage", record.tenthPercentage)
                    ReviewRow("Total Marks", record.tenthTotalMarks)
                    ReviewRow("Board", record.tenthBoardName)
                    ReviewRow("School", record.tenthSchoolName)
                    ReviewRow("School Address", record.tenthSchoolAddress)
                    ReviewRow("Area PIN", record.tenthAreaPinCode)
                }
            }

            item {
                SectionCard(title = strings.sectionC) {
                    ReviewRow("Passing Year", record.twelfthPassingYear)
                    ReviewRow("Roll Number", record.twelfthRollNumber)
                    ReviewRow("Percentage", record.twelfthPercentage)
                    ReviewRow("Total Marks", record.twelfthTotalMarks)
                    ReviewRow("Board", record.twelfthBoardName)
                    ReviewRow("School", record.twelfthSchoolName)
                    ReviewRow("School Address", record.twelfthSchoolAddress)
                    ReviewRow("Area PIN", record.twelfthAreaPinCode)
                }
            }

            item {
                SectionCard(title = strings.sectionD) {
                    ReviewRow("1st Choice", record.examCityChoice1)
                    ReviewRow("2nd Choice", record.examCityChoice2)
                    ReviewRow("3rd Choice", record.examCityChoice3)
                }
            }

            item {
                SectionCard(title = strings.sectionE) {
                    ReviewRow("Father Qualification", record.fatherQualification)
                    ReviewRow("Father Occupation", record.fatherOccupation)
                    ReviewRow("Mother Qualification", record.motherQualification)
                    ReviewRow("Mother Occupation", record.motherOccupation)
                }
            }

            item {
                SectionCard(title = strings.sectionF) {
                    ReviewRow("Aadhaar Address", record.fullAadhaarAddress)
                    ReviewRow("PIN Code", record.addressPinCode)
                }
            }

            item {
                SectionCard(title = strings.sectionG) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DocThumbPreview("Photo", record.photoPath, Modifier.weight(1f))
                        DocThumbPreview("Signature", record.signaturePath, Modifier.weight(1f))
                        DocThumbPreview("Aadhaar", record.aadhaarPhotoPath, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DocThumbPreview("10th Marksheet", record.tenthMarksheetPath, Modifier.weight(1f))
                        DocThumbPreview("12th Marksheet", record.twelfthMarksheetPath, Modifier.weight(1f))
                        DocThumbPreview("Cat. Cert", record.categoryCertificatePath, Modifier.weight(1f))
                    }
                }
            }
        }
    }

    savedStudentId?.let { id ->
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text(strings.formSavedTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.formSavedMessage) },
            confirmButton = {
                Button(onClick = {
                    savedStudentId = null
                    onOpenPrintPreview(id)
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.goToPrintPreview)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    savedStudentId = null
                    onGoDashboard()
                }) {
                    Text(strings.backToDashboard)
                }
            }
        )
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.42f)
            )
            Text(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.58f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun DocThumbPreview(label: String, path: String, modifier: Modifier = Modifier) {
    val exists = path.isNotBlank() && File(path).exists()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (exists) {
                AsyncImage(
                    model = File(path),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Spacer(Modifier.height(64.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}
