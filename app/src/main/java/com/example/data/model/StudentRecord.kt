package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object FormStatus {
    const val PENDING = "PENDING"
    const val COMPLETED = "COMPLETED"
}

enum class DocumentSlot(val filePrefix: String) {
    PHOTO("photo"),
    SIGNATURE("signature"),
    AADHAAR_PHOTO("aadhaar"),
    TENTH_MARKSHEET("tenth"),
    TWELFTH_MARKSHEET("twelfth"),
    CATEGORY_CERT("category_cert")
}

@Entity(tableName = "student_records")
data class StudentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val formNumber: String = "",
    val status: String = FormStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // A. Personal Details
    val studentName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val aadhaarNumber: String = "",
    val studentEmail: String = "",
    val studentMobile: String = "",
    val emergencyEmail: String = "",
    val emergencyMobile: String = "",
    val category: String = "",
    val religion: String = "",
    val personWithDisability: String = "No",
    val categoryCertificateId: String = "",
    val categoryCertificate: String = "",
    val issuingAuthorityName: String = "",
    val certificateIssueDate: String = "",
    val domicileState: String = "",
    val familyAnnualIncome: String = "",
    val questionPaperMedium: String = "English",
    val onlyGirlChild: String = "No",

    // B. Matriculation (10th) Details
    val tenthPassingYear: String = "",
    val tenthRollNumber: String = "",
    val tenthPercentage: String = "",
    val tenthTotalMarks: String = "",
    val tenthBoardName: String = "",
    val tenthSchoolName: String = "",
    val tenthSchoolAddress: String = "",
    val tenthAreaPinCode: String = "",

    // C. Higher Secondary (12th) Details
    val twelfthPassingYear: String = "",
    val twelfthRollNumber: String = "",
    val twelfthPercentage: String = "",
    val twelfthTotalMarks: String = "",
    val twelfthBoardName: String = "",
    val twelfthSchoolName: String = "",
    val twelfthSchoolAddress: String = "",
    val twelfthAreaPinCode: String = "",

    // D. NEET Exam City Preferences
    val examCityChoice1: String = "",
    val examCityChoice2: String = "",
    val examCityChoice3: String = "",

    // E. Guardian / Parents Details
    val fatherQualification: String = "",
    val fatherOccupation: String = "",
    val motherQualification: String = "",
    val motherOccupation: String = "",

    // F. Address Details
    val fullAadhaarAddress: String = "",
    val addressPinCode: String = "",

    // G. Documents (Local Internal File Paths)
    val photoPath: String = "",
    val signaturePath: String = "",
    val aadhaarPhotoPath: String = "",
    val tenthMarksheetPath: String = "",
    val twelfthMarksheetPath: String = "",
    val categoryCertificatePath: String = ""
) {
    fun documentPathFor(slot: DocumentSlot): String = when (slot) {
        DocumentSlot.PHOTO -> photoPath
        DocumentSlot.SIGNATURE -> signaturePath
        DocumentSlot.AADHAAR_PHOTO -> aadhaarPhotoPath
        DocumentSlot.TENTH_MARKSHEET -> tenthMarksheetPath
        DocumentSlot.TWELFTH_MARKSHEET -> twelfthMarksheetPath
        DocumentSlot.CATEGORY_CERT -> categoryCertificatePath
    }

    fun withDocumentPath(slot: DocumentSlot, path: String): StudentRecord = when (slot) {
        DocumentSlot.PHOTO -> copy(photoPath = path)
        DocumentSlot.SIGNATURE -> copy(signaturePath = path)
        DocumentSlot.AADHAAR_PHOTO -> copy(aadhaarPhotoPath = path)
        DocumentSlot.TENTH_MARKSHEET -> copy(tenthMarksheetPath = path)
        DocumentSlot.TWELFTH_MARKSHEET -> copy(twelfthMarksheetPath = path)
        DocumentSlot.CATEGORY_CERT -> copy(categoryCertificatePath = path)
    }

    fun allDocumentPaths(): List<String> = listOf(
        photoPath,
        signaturePath,
        aadhaarPhotoPath,
        tenthMarksheetPath,
        twelfthMarksheetPath,
        categoryCertificatePath
    ).filter { it.isNotBlank() }
}
