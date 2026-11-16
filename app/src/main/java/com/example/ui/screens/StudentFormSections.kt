package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DocumentSlot
import com.example.data.model.StudentRecord
import com.example.domain.FormOptions
import com.example.domain.FormValidation.Fields
import com.example.domain.ValidationErrorType
import com.example.domain.ValidationResult
import com.example.i18n.LocalAppStrings
import com.example.ui.components.FormDateField
import com.example.ui.components.FormDropdownField
import com.example.ui.components.FormFieldText
import com.example.ui.components.SectionCard
import com.example.ui.components.validationMessage
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import java.io.File

@Composable
fun PersonalDetailsSection(
    record: StudentRecord,
    validation: ValidationResult,
    onUpdate: ((StudentRecord) -> StudentRecord) -> Unit
) {
    val strings = LocalAppStrings.current
    val errs = validation.fieldErrors

    SectionCard(title = strings.sectionA) {
        FormFieldText(
            label = strings.studentName,
            value = record.studentName,
            onValueChange = { v -> onUpdate { it.copy(studentName = v.uppercase()) } },
            testTag = "input_student_name",
            errorType = errs[Fields.STUDENT_NAME],
            helperText = strings.studentNameHint,
            capitalization = KeyboardCapitalization.Characters
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormDateField(
                label = strings.dateOfBirth,
                value = record.dateOfBirth,
                onValueChange = { v -> onUpdate { it.copy(dateOfBirth = v) } },
                testTag = "input_dob",
                errorType = errs[Fields.DOB],
                modifier = Modifier.weight(1.2f)
            )
            FormDropdownField(
                label = strings.gender,
                value = record.gender,
                options = FormOptions.genders,
                onOptionSelected = { v -> onUpdate { it.copy(gender = v) } },
                testTag = "input_gender",
                errorType = errs[Fields.GENDER],
                modifier = Modifier.weight(1f)
            )
        }

        FormFieldText(
            label = strings.fatherName,
            value = record.fatherName,
            onValueChange = { v -> onUpdate { it.copy(fatherName = v) } },
            testTag = "input_father_name",
            errorType = errs[Fields.FATHER_NAME]
        )

        FormFieldText(
            label = strings.motherName,
            value = record.motherName,
            onValueChange = { v -> onUpdate { it.copy(motherName = v) } },
            testTag = "input_mother_name",
            errorType = errs[Fields.MOTHER_NAME]
        )

        FormFieldText(
            label = strings.aadhaarNumber,
            value = record.aadhaarNumber,
            onValueChange = { v -> onUpdate { it.copy(aadhaarNumber = v) } },
            testTag = "input_aadhaar",
            errorType = errs[Fields.AADHAAR],
            keyboardType = KeyboardType.Number,
            maxLength = 12
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormFieldText(
                label = strings.studentMobile,
                value = record.studentMobile,
                onValueChange = { v -> onUpdate { it.copy(studentMobile = v) } },
                testTag = "input_student_mobile",
                errorType = errs[Fields.STUDENT_MOBILE],
                keyboardType = KeyboardType.Phone,
                maxLength = 10,
                modifier = Modifier.weight(1f)
            )
            FormFieldText(
                label = strings.emergencyMobile,
                value = record.emergencyMobile,
                onValueChange = { v -> onUpdate { it.copy(emergencyMobile = v) } },
                testTag = "input_emergency_mobile",
                errorType = errs[Fields.EMERGENCY_MOBILE],
                keyboardType = KeyboardType.Phone,
                maxLength = 10,
                modifier = Modifier.weight(1f)
            )
        }

        FormFieldText(
            label = strings.studentEmail,
            value = record.studentEmail,
            onValueChange = { v -> onUpdate { it.copy(studentEmail = v.trim()) } },
            testTag = "input_student_email",
            errorType = errs[Fields.STUDENT_EMAIL],
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None
        )

        FormFieldText(
            label = strings.emergencyEmail,
            value = record.emergencyEmail,
            onValueChange = { v -> onUpdate { it.copy(emergencyEmail = v.trim()) } },
            testTag = "input_emergency_email",
            errorType = errs[Fields.EMERGENCY_EMAIL],
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormDropdownField(
                label = strings.category,
                value = record.category,
                options = FormOptions.categories,
                onOptionSelected = { v -> onUpdate { it.copy(category = v) } },
                testTag = "input_category",
                errorType = errs[Fields.CATEGORY],
                modifier = Modifier.weight(1f)
            )
            FormDropdownField(
                label = strings.religion,
                value = record.religion,
                options = FormOptions.religions,
                onOptionSelected = { v -> onUpdate { it.copy(religion = v) } },
                testTag = "input_religion",
                errorType = errs[Fields.RELIGION],
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormDropdownField(
                label = strings.personWithDisability,
                value = record.personWithDisability,
                options = FormOptions.yesNo,
                onOptionSelected = { v -> onUpdate { it.copy(personWithDisability = v) } },
                testTag = "input_pwd",
                errorType = errs[Fields.PWD],
                modifier = Modifier.weight(1f)
            )
            FormDropdownField(
                label = strings.onlyGirlChild,
                value = record.onlyGirlChild,
                options = FormOptions.yesNo,
                onOptionSelected = { v -> onUpdate { it.copy(onlyGirlChild = v) } },
                testTag = "input_only_girl_child",
                errorType = errs[Fields.ONLY_GIRL_CHILD],
                modifier = Modifier.weight(1f)
            )
        }

        FormDropdownField(
            label = strings.categoryCertStatus,
            value = record.categoryCertificate,
            options = FormOptions.categoryCertOptions,
            onOptionSelected = { v -> onUpdate { it.copy(categoryCertificate = v) } },
            testTag = "input_category_cert_status"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormFieldText(
                label = strings.categoryCertId,
                value = record.categoryCertificateId,
                onValueChange = { v -> onUpdate { it.copy(categoryCertificateId = v) } },
                testTag = "input_category_cert_id",
                capitalization = KeyboardCapitalization.Characters,
                modifier = Modifier.weight(1f)
            )
            FormDateField(
                label = strings.certIssueDate,
                value = record.certificateIssueDate,
                onValueChange = { v -> onUpdate { it.copy(certificateIssueDate = v) } },
                testTag = "input_cert_issue_date",
                errorType = errs[Fields.CERT_DATE],
                modifier = Modifier.weight(1f)
            )
        }

        FormFieldText(
            label = strings.issuingAuthority,
            value = record.issuingAuthorityName,
            onValueChange = { v -> onUpdate { it.copy(issuingAuthorityName = v) } },
            testTag = "input_issuing_authority"
        )

        FormDropdownField(
            label = strings.domicileState,
            value = record.domicileState,
            options = FormOptions.indianStates,
            onOptionSelected = { v -> onUpdate { it.copy(domicileState = v) } },
            testTag = "input_domicile_state",
            errorType = errs[Fields.DOMICILE_STATE]
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormDropdownField(
                label = strings.familyAnnualIncome,
                value = record.familyAnnualIncome,
                options = FormOptions.annualIncomeRanges,
                onOptionSelected = { v -> onUpdate { it.copy(familyAnnualIncome = v) } },
                testTag = "input_annual_income",
                errorType = errs[Fields.ANNUAL_INCOME],
                modifier = Modifier.weight(1.2f)
            )
            FormDropdownField(
                label = strings.questionPaperMedium,
                value = record.questionPaperMedium,
                options = FormOptions.questionMediums,
                onOptionSelected = { v -> onUpdate { it.copy(questionPaperMedium = v) } },
                testTag = "input_question_medium",
                errorType = errs[Fields.QUESTION_MEDIUM],
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TenthDetailsSection(
    record: StudentRecord,
    validation: ValidationResult,
    onUpdate: ((StudentRecord) -> StudentRecord) -> Unit
) {
    val strings = LocalAppStrings.current
    val errs = validation.fieldErrors

    SectionCard(title = strings.sectionB) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormDropdownField(
                label = strings.passingYear,
                value = record.tenthPassingYear,
                options = FormOptions.passingYears,
                onOptionSelected = { v -> onUpdate { it.copy(tenthPassingYear = v) } },
                testTag = "input_tenth_year",
                errorType = errs[Fields.TENTH_YEAR],
                modifier = Modifier.weight(1f)
            )
            FormFieldText(
                label = strings.rollNumber,
                value = record.tenthRollNumber,
                onValueChange = { v -> onUpdate { it.copy(tenthRollNumber = v) } },
                testTag = "input_tenth_roll",
                errorType = errs[Fields.TENTH_ROLL],
                capitalization = KeyboardCapitalization.Characters,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormFieldText(
                label = strings.percentage,
                value = record.tenthPercentage,
                onValueChange = { v -> onUpdate { it.copy(tenthPercentage = v) } },
                testTag = "input_tenth_percent",
                errorType = errs[Fields.TENTH_PERCENT],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            FormFieldText(
                label = strings.totalMarks,
                value = record.tenthTotalMarks,
                onValueChange = { v -> onUpdate { it.copy(tenthTotalMarks = v) } },
                testTag = "input_tenth_marks",
                errorType = errs[Fields.TENTH_MARKS],
                modifier = Modifier.weight(1f)
            )
        }
        FormDropdownField(
            label = strings.boardName,
            value = record.tenthBoardName,
            options = FormOptions.boards,
            onOptionSelected = { v -> onUpdate { it.copy(tenthBoardName = v) } },
            testTag = "input_tenth_board",
            errorType = errs[Fields.TENTH_BOARD],
            allowCustomTyping = true
        )
        FormFieldText(
            label = strings.schoolName,
            value = record.tenthSchoolName,
            onValueChange = { v -> onUpdate { it.copy(tenthSchoolName = v) } },
            testTag = "input_tenth_school",
            errorType = errs[Fields.TENTH_SCHOOL]
        )
        FormFieldText(
            label = strings.schoolAddress,
            value = record.tenthSchoolAddress,
            onValueChange = { v -> onUpdate { it.copy(tenthSchoolAddress = v) } },
            testTag = "input_tenth_address",
            errorType = errs[Fields.TENTH_ADDRESS],
            singleLine = false,
            minLines = 2
        )
        FormFieldText(
            label = strings.areaPinCode,
            value = record.tenthAreaPinCode,
            onValueChange = { v -> onUpdate { it.copy(tenthAreaPinCode = v) } },
            testTag = "input_tenth_pin",
            errorType = errs[Fields.TENTH_PIN],
            keyboardType = KeyboardType.Number,
            maxLength = 6
        )
    }
}

@Composable
fun TwelfthDetailsSection(
    record: StudentRecord,
    validation: ValidationResult,
    onUpdate: ((StudentRecord) -> StudentRecord) -> Unit
) {
    val strings = LocalAppStrings.current
    val errs = validation.fieldErrors

    SectionCard(title = strings.sectionC) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormDropdownField(
                label = strings.passingYear,
                value = record.twelfthPassingYear,
                options = FormOptions.passingYears,
                onOptionSelected = { v -> onUpdate { it.copy(twelfthPassingYear = v) } },
                testTag = "input_twelfth_year",
                errorType = errs[Fields.TWELFTH_YEAR],
                modifier = Modifier.weight(1f)
            )
            FormFieldText(
                label = strings.rollNumber,
                value = record.twelfthRollNumber,
                onValueChange = { v -> onUpdate { it.copy(twelfthRollNumber = v) } },
                testTag = "input_twelfth_roll",
                errorType = errs[Fields.TWELFTH_ROLL],
                capitalization = KeyboardCapitalization.Characters,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormFieldText(
                label = strings.percentage,
                value = record.twelfthPercentage,
                onValueChange = { v -> onUpdate { it.copy(twelfthPercentage = v) } },
                testTag = "input_twelfth_percent",
                errorType = errs[Fields.TWELFTH_PERCENT],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            FormFieldText(
                label = strings.totalMarks,
                value = record.twelfthTotalMarks,
                onValueChange = { v -> onUpdate { it.copy(twelfthTotalMarks = v) } },
                testTag = "input_twelfth_marks",
                errorType = errs[Fields.TWELFTH_MARKS],
                modifier = Modifier.weight(1f)
            )
        }
        FormDropdownField(
            label = strings.boardName,
            value = record.twelfthBoardName,
            options = FormOptions.boards,
            onOptionSelected = { v -> onUpdate { it.copy(twelfthBoardName = v) } },
            testTag = "input_twelfth_board",
            errorType = errs[Fields.TWELFTH_BOARD],
            allowCustomTyping = true
        )
        FormFieldText(
            label = strings.schoolName,
            value = record.twelfthSchoolName,
            onValueChange = { v -> onUpdate { it.copy(twelfthSchoolName = v) } },
            testTag = "input_twelfth_school",
            errorType = errs[Fields.TWELFTH_SCHOOL]
        )
        FormFieldText(
            label = strings.schoolAddress,
            value = record.twelfthSchoolAddress,
            onValueChange = { v -> onUpdate { it.copy(twelfthSchoolAddress = v) } },
            testTag = "input_twelfth_address",
            errorType = errs[Fields.TWELFTH_ADDRESS],
            singleLine = false,
            minLines = 2
        )
        FormFieldText(
            label = strings.areaPinCode,
            value = record.twelfthAreaPinCode,
            onValueChange = { v -> onUpdate { it.copy(twelfthAreaPinCode = v) } },
            testTag = "input_twelfth_pin",
            errorType = errs[Fields.TWELFTH_PIN],
            keyboardType = KeyboardType.Number,
            maxLength = 6
        )
    }
}

@Composable
fun ExamCitiesSection(
    record: StudentRecord,
    validation: ValidationResult,
    onUpdate: ((StudentRecord) -> StudentRecord) -> Unit
) {
    val strings = LocalAppStrings.current
    val errs = validation.fieldErrors

    SectionCard(title = strings.sectionD) {
        FormDropdownField(
            label = strings.firstChoice,
            value = record.examCityChoice1,
            options = FormOptions.popularExamCities,
            onOptionSelected = { v -> onUpdate { it.copy(examCityChoice1 = v) } },
            testTag = "input_city_1",
            errorType = errs[Fields.CITY_1],
            allowCustomTyping = true
        )
        FormDropdownField(
            label = strings.secondChoice,
            value = record.examCityChoice2,
            options = FormOptions.popularExamCities,
            onOptionSelected = { v -> onUpdate { it.copy(examCityChoice2 = v) } },
            testTag = "input_city_2",
            errorType = errs[Fields.CITY_2],
            allowCustomTyping = true
        )
        FormDropdownField(
            label = strings.thirdChoice,
            value = record.examCityChoice3,
            options = FormOptions.popularExamCities,
            onOptionSelected = { v -> onUpdate { it.copy(examCityChoice3 = v) } },
            testTag = "input_city_3",
            errorType = errs[Fields.CITY_3],
            allowCustomTyping = true
        )
    }
}

@Composable
fun ParentsDetailsSection(
    record: StudentRecord,
    validation: ValidationResult,
    onUpdate: ((StudentRecord) -> StudentRecord) -> Unit
) {
    val strings = LocalAppStrings.current
    val errs = validation.fieldErrors

    SectionCard(title = strings.sectionE) {
        Text(
            text = strings.fatherDetails,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        FormDropdownField(
            label = strings.qualification,
            value = record.fatherQualification,
            options = FormOptions.qualifications,
            onOptionSelected = { v -> onUpdate { it.copy(fatherQualification = v) } },
            testTag = "input_father_qual",
            errorType = errs[Fields.FATHER_QUAL],
            allowCustomTyping = true
        )
        FormDropdownField(
            label = strings.occupation,
            value = record.fatherOccupation,
            options = FormOptions.occupations,
            onOptionSelected = { v -> onUpdate { it.copy(fatherOccupation = v) } },
            testTag = "input_father_occ",
            errorType = errs[Fields.FATHER_OCC],
            allowCustomTyping = true
        )

        Spacer(Modifier.height(4.dp))
        Text(
            text = strings.motherDetails,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        FormDropdownField(
            label = strings.qualification,
            value = record.motherQualification,
            options = FormOptions.qualifications,
            onOptionSelected = { v -> onUpdate { it.copy(motherQualification = v) } },
            testTag = "input_mother_qual",
            errorType = errs[Fields.MOTHER_QUAL],
            allowCustomTyping = true
        )
        FormDropdownField(
            label = strings.occupation,
            value = record.motherOccupation,
            options = FormOptions.occupations,
            onOptionSelected = { v -> onUpdate { it.copy(motherOccupation = v) } },
            testTag = "input_mother_occ",
            errorType = errs[Fields.MOTHER_OCC],
            allowCustomTyping = true
        )
    }
}

@Composable
fun AddressDetailsSection(
    record: StudentRecord,
    validation: ValidationResult,
    onUpdate: ((StudentRecord) -> StudentRecord) -> Unit
) {
    val strings = LocalAppStrings.current
    val errs = validation.fieldErrors

    SectionCard(title = strings.sectionF) {
        FormFieldText(
            label = strings.fullAadhaarAddress,
            value = record.fullAadhaarAddress,
            onValueChange = { v -> onUpdate { it.copy(fullAadhaarAddress = v) } },
            testTag = "input_full_address",
            errorType = errs[Fields.FULL_ADDRESS],
            singleLine = false,
            minLines = 3
        )
        FormFieldText(
            label = strings.pinCode,
            value = record.addressPinCode,
            onValueChange = { v -> onUpdate { it.copy(addressPinCode = v) } },
            testTag = "input_address_pin",
            errorType = errs[Fields.ADDRESS_PIN],
            keyboardType = KeyboardType.Number,
            maxLength = 6
        )
    }
}

@Composable
fun DocumentsSection(
    record: StudentRecord,
    validation: ValidationResult,
    onPickGallery: (DocumentSlot) -> Unit,
    onTakeCamera: (DocumentSlot) -> Unit,
    onRemove: (DocumentSlot) -> Unit
) {
    val strings = LocalAppStrings.current
    val errs = validation.fieldErrors

    SectionCard(title = strings.sectionG) {
        DocumentUploadCard(
            title = strings.docPhoto,
            path = record.photoPath,
            isOptional = false,
            errorType = errs[Fields.DOC_PHOTO],
            onPickGallery = { onPickGallery(DocumentSlot.PHOTO) },
            onTakeCamera = { onTakeCamera(DocumentSlot.PHOTO) },
            onRemove = { onRemove(DocumentSlot.PHOTO) },
            testTag = "doc_photo"
        )
        DocumentUploadCard(
            title = strings.docSignature,
            path = record.signaturePath,
            isOptional = false,
            errorType = errs[Fields.DOC_SIGNATURE],
            onPickGallery = { onPickGallery(DocumentSlot.SIGNATURE) },
            onTakeCamera = { onTakeCamera(DocumentSlot.SIGNATURE) },
            onRemove = { onRemove(DocumentSlot.SIGNATURE) },
            testTag = "doc_signature"
        )
        DocumentUploadCard(
            title = strings.docAadhaar,
            path = record.aadhaarPhotoPath,
            isOptional = false,
            errorType = errs[Fields.DOC_AADHAAR],
            onPickGallery = { onPickGallery(DocumentSlot.AADHAAR_PHOTO) },
            onTakeCamera = { onTakeCamera(DocumentSlot.AADHAAR_PHOTO) },
            onRemove = { onRemove(DocumentSlot.AADHAAR_PHOTO) },
            testTag = "doc_aadhaar"
        )
        DocumentUploadCard(
            title = strings.docTenth,
            path = record.tenthMarksheetPath,
            isOptional = false,
            errorType = errs[Fields.DOC_TENTH],
            onPickGallery = { onPickGallery(DocumentSlot.TENTH_MARKSHEET) },
            onTakeCamera = { onTakeCamera(DocumentSlot.TENTH_MARKSHEET) },
            onRemove = { onRemove(DocumentSlot.TENTH_MARKSHEET) },
            testTag = "doc_tenth"
        )
        DocumentUploadCard(
            title = strings.docTwelfth,
            path = record.twelfthMarksheetPath,
            isOptional = false,
            errorType = errs[Fields.DOC_TWELFTH],
            onPickGallery = { onPickGallery(DocumentSlot.TWELFTH_MARKSHEET) },
            onTakeCamera = { onTakeCamera(DocumentSlot.TWELFTH_MARKSHEET) },
            onRemove = { onRemove(DocumentSlot.TWELFTH_MARKSHEET) },
            testTag = "doc_twelfth"
        )
        DocumentUploadCard(
            title = strings.docCategoryCert,
            path = record.categoryCertificatePath,
            isOptional = true,
            errorType = null,
            onPickGallery = { onPickGallery(DocumentSlot.CATEGORY_CERT) },
            onTakeCamera = { onTakeCamera(DocumentSlot.CATEGORY_CERT) },
            onRemove = { onRemove(DocumentSlot.CATEGORY_CERT) },
            testTag = "doc_category"
        )
    }
}

@Composable
private fun DocumentUploadCard(
    title: String,
    path: String,
    isOptional: Boolean,
    errorType: ValidationErrorType?,
    onPickGallery: () -> Unit,
    onTakeCamera: () -> Unit,
    onRemove: () -> Unit,
    testTag: String
) {
    val strings = LocalAppStrings.current
    val hasFile = path.isNotBlank() && File(path).exists()
    val borderColor = when {
        errorType != null -> MaterialTheme.colorScheme.error
        hasFile -> SuccessGreen
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOptional) strings.optionalTag else strings.requiredTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hasFile) {
                    Surface(color = SuccessGreenBg, shape = RoundedCornerShape(50.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = strings.uploaded,
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (hasFile) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (errorType != null) validationMessage(errorType, strings) else "No image selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (errorType != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPickGallery,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.pickFromGallery, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onTakeCamera,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.takePhoto, style = MaterialTheme.typography.labelMedium)
                }
                if (hasFile) {
                    OutlinedButton(
                        onClick = onRemove,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = strings.removeDoc,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
