package com.example.domain

import com.example.data.model.StudentRecord

enum class FormSection(val index: Int) {
    PERSONAL(0),
    TENTH(1),
    TWELFTH(2),
    EXAM_CITY(3),
    PARENTS(4),
    ADDRESS(5),
    DOCUMENTS(6)
}

enum class ValidationErrorType {
    REQUIRED,
    INVALID_MOBILE,
    INVALID_EMAIL,
    INVALID_AADHAAR,
    INVALID_PIN,
    INVALID_YEAR,
    INVALID_DOB
}

data class ValidationResult(
    val fieldErrors: Map<String, ValidationErrorType> = emptyMap(),
    val missingBySection: Map<FormSection, Int> = emptyMap()
) {
    val isValid: Boolean get() = fieldErrors.isEmpty()
    val totalErrors: Int get() = fieldErrors.size
}

object FormValidation {
    private val mobileRegex = Regex("^[6-9]\\d{9}$")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val aadhaarRegex = Regex("^\\d{12}$")
    private val pinRegex = Regex("^\\d{6}$")
    private val yearRegex = Regex("^(19|20)\\d{2}$")
    private val dobRegex = Regex("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19|20)\\d{2}$")

    object Fields {
        const val STUDENT_NAME = "studentName"
        const val DOB = "dateOfBirth"
        const val GENDER = "gender"
        const val FATHER_NAME = "fatherName"
        const val MOTHER_NAME = "motherName"
        const val AADHAAR = "aadhaarNumber"
        const val STUDENT_EMAIL = "studentEmail"
        const val STUDENT_MOBILE = "studentMobile"
        const val EMERGENCY_EMAIL = "emergencyEmail"
        const val EMERGENCY_MOBILE = "emergencyMobile"
        const val CATEGORY = "category"
        const val RELIGION = "religion"
        const val PWD = "personWithDisability"
        const val CERT_DATE = "certificateIssueDate"
        const val DOMICILE_STATE = "domicileState"
        const val ANNUAL_INCOME = "familyAnnualIncome"
        const val QUESTION_MEDIUM = "questionPaperMedium"
        const val ONLY_GIRL_CHILD = "onlyGirlChild"

        const val TENTH_YEAR = "tenthPassingYear"
        const val TENTH_ROLL = "tenthRollNumber"
        const val TENTH_PERCENT = "tenthPercentage"
        const val TENTH_MARKS = "tenthTotalMarks"
        const val TENTH_BOARD = "tenthBoardName"
        const val TENTH_SCHOOL = "tenthSchoolName"
        const val TENTH_ADDRESS = "tenthSchoolAddress"
        const val TENTH_PIN = "tenthAreaPinCode"

        const val TWELFTH_YEAR = "twelfthPassingYear"
        const val TWELFTH_ROLL = "twelfthRollNumber"
        const val TWELFTH_PERCENT = "twelfthPercentage"
        const val TWELFTH_MARKS = "twelfthTotalMarks"
        const val TWELFTH_BOARD = "twelfthBoardName"
        const val TWELFTH_SCHOOL = "twelfthSchoolName"
        const val TWELFTH_ADDRESS = "twelfthSchoolAddress"
        const val TWELFTH_PIN = "twelfthAreaPinCode"

        const val CITY_1 = "examCityChoice1"
        const val CITY_2 = "examCityChoice2"
        const val CITY_3 = "examCityChoice3"

        const val FATHER_QUAL = "fatherQualification"
        const val FATHER_OCC = "fatherOccupation"
        const val MOTHER_QUAL = "motherQualification"
        const val MOTHER_OCC = "motherOccupation"

        const val FULL_ADDRESS = "fullAadhaarAddress"
        const val ADDRESS_PIN = "addressPinCode"

        const val DOC_PHOTO = "photoPath"
        const val DOC_SIGNATURE = "signaturePath"
        const val DOC_AADHAAR = "aadhaarPhotoPath"
        const val DOC_TENTH = "tenthMarksheetPath"
        const val DOC_TWELFTH = "twelfthMarksheetPath"
    }

    fun isValidMobile(value: String): Boolean = mobileRegex.matches(value.trim())
    fun isValidEmail(value: String): Boolean = emailRegex.matches(value.trim())
    fun isValidAadhaar(value: String): Boolean = aadhaarRegex.matches(value.trim())
    fun isValidPin(value: String): Boolean = pinRegex.matches(value.trim())
    fun isValidDob(value: String): Boolean = dobRegex.matches(value.trim())

    fun validateForCompletion(record: StudentRecord): ValidationResult {
        val errors = mutableMapOf<String, ValidationErrorType>()
        val sectionCounts = mutableMapOf<FormSection, Int>()

        fun mark(section: FormSection, field: String, error: ValidationErrorType) {
            errors[field] = error
            sectionCounts[section] = (sectionCounts[section] ?: 0) + 1
        }

        fun checkRequired(section: FormSection, field: String, value: String) {
            if (value.isBlank()) mark(section, field, ValidationErrorType.REQUIRED)
        }

        // A. Personal Details
        checkRequired(FormSection.PERSONAL, Fields.STUDENT_NAME, record.studentName)
        if (record.dateOfBirth.isBlank()) {
            mark(FormSection.PERSONAL, Fields.DOB, ValidationErrorType.REQUIRED)
        } else if (!isValidDob(record.dateOfBirth)) {
            mark(FormSection.PERSONAL, Fields.DOB, ValidationErrorType.INVALID_DOB)
        }
        checkRequired(FormSection.PERSONAL, Fields.GENDER, record.gender)
        checkRequired(FormSection.PERSONAL, Fields.FATHER_NAME, record.fatherName)
        checkRequired(FormSection.PERSONAL, Fields.MOTHER_NAME, record.motherName)

        if (record.aadhaarNumber.isBlank()) {
            mark(FormSection.PERSONAL, Fields.AADHAAR, ValidationErrorType.REQUIRED)
        } else if (!isValidAadhaar(record.aadhaarNumber)) {
            mark(FormSection.PERSONAL, Fields.AADHAAR, ValidationErrorType.INVALID_AADHAAR)
        }

        if (record.studentEmail.isBlank()) {
            mark(FormSection.PERSONAL, Fields.STUDENT_EMAIL, ValidationErrorType.REQUIRED)
        } else if (!isValidEmail(record.studentEmail)) {
            mark(FormSection.PERSONAL, Fields.STUDENT_EMAIL, ValidationErrorType.INVALID_EMAIL)
        }

        if (record.studentMobile.isBlank()) {
            mark(FormSection.PERSONAL, Fields.STUDENT_MOBILE, ValidationErrorType.REQUIRED)
        } else if (!isValidMobile(record.studentMobile)) {
            mark(FormSection.PERSONAL, Fields.STUDENT_MOBILE, ValidationErrorType.INVALID_MOBILE)
        }

        if (record.emergencyEmail.isNotBlank() && !isValidEmail(record.emergencyEmail)) {
            mark(FormSection.PERSONAL, Fields.EMERGENCY_EMAIL, ValidationErrorType.INVALID_EMAIL)
        }

        if (record.emergencyMobile.isBlank()) {
            mark(FormSection.PERSONAL, Fields.EMERGENCY_MOBILE, ValidationErrorType.REQUIRED)
        } else if (!isValidMobile(record.emergencyMobile)) {
            mark(FormSection.PERSONAL, Fields.EMERGENCY_MOBILE, ValidationErrorType.INVALID_MOBILE)
        }

        checkRequired(FormSection.PERSONAL, Fields.CATEGORY, record.category)
        checkRequired(FormSection.PERSONAL, Fields.RELIGION, record.religion)
        checkRequired(FormSection.PERSONAL, Fields.PWD, record.personWithDisability)

        if (record.certificateIssueDate.isNotBlank() && !isValidDob(record.certificateIssueDate)) {
            mark(FormSection.PERSONAL, Fields.CERT_DATE, ValidationErrorType.INVALID_DOB)
        }

        checkRequired(FormSection.PERSONAL, Fields.DOMICILE_STATE, record.domicileState)
        checkRequired(FormSection.PERSONAL, Fields.ANNUAL_INCOME, record.familyAnnualIncome)
        checkRequired(FormSection.PERSONAL, Fields.QUESTION_MEDIUM, record.questionPaperMedium)
        checkRequired(FormSection.PERSONAL, Fields.ONLY_GIRL_CHILD, record.onlyGirlChild)

        // B. 10th Details
        if (record.tenthPassingYear.isBlank()) {
            mark(FormSection.TENTH, Fields.TENTH_YEAR, ValidationErrorType.REQUIRED)
        } else if (!yearRegex.matches(record.tenthPassingYear.trim())) {
            mark(FormSection.TENTH, Fields.TENTH_YEAR, ValidationErrorType.INVALID_YEAR)
        }
        checkRequired(FormSection.TENTH, Fields.TENTH_ROLL, record.tenthRollNumber)
        checkRequired(FormSection.TENTH, Fields.TENTH_PERCENT, record.tenthPercentage)
        checkRequired(FormSection.TENTH, Fields.TENTH_MARKS, record.tenthTotalMarks)
        checkRequired(FormSection.TENTH, Fields.TENTH_BOARD, record.tenthBoardName)
        checkRequired(FormSection.TENTH, Fields.TENTH_SCHOOL, record.tenthSchoolName)
        checkRequired(FormSection.TENTH, Fields.TENTH_ADDRESS, record.tenthSchoolAddress)
        if (record.tenthAreaPinCode.isBlank()) {
            mark(FormSection.TENTH, Fields.TENTH_PIN, ValidationErrorType.REQUIRED)
        } else if (!isValidPin(record.tenthAreaPinCode)) {
            mark(FormSection.TENTH, Fields.TENTH_PIN, ValidationErrorType.INVALID_PIN)
        }

        // C. 12th Details
        if (record.twelfthPassingYear.isBlank()) {
            mark(FormSection.TWELFTH, Fields.TWELFTH_YEAR, ValidationErrorType.REQUIRED)
        } else if (!yearRegex.matches(record.twelfthPassingYear.trim())) {
            mark(FormSection.TWELFTH, Fields.TWELFTH_YEAR, ValidationErrorType.INVALID_YEAR)
        }
        checkRequired(FormSection.TWELFTH, Fields.TWELFTH_ROLL, record.twelfthRollNumber)
        checkRequired(FormSection.TWELFTH, Fields.TWELFTH_PERCENT, record.twelfthPercentage)
        checkRequired(FormSection.TWELFTH, Fields.TWELFTH_MARKS, record.twelfthTotalMarks)
        checkRequired(FormSection.TWELFTH, Fields.TWELFTH_BOARD, record.twelfthBoardName)
        checkRequired(FormSection.TWELFTH, Fields.TWELFTH_SCHOOL, record.twelfthSchoolName)
        checkRequired(FormSection.TWELFTH, Fields.TWELFTH_ADDRESS, record.twelfthSchoolAddress)
        if (record.twelfthAreaPinCode.isBlank()) {
            mark(FormSection.TWELFTH, Fields.TWELFTH_PIN, ValidationErrorType.REQUIRED)
        } else if (!isValidPin(record.twelfthAreaPinCode)) {
            mark(FormSection.TWELFTH, Fields.TWELFTH_PIN, ValidationErrorType.INVALID_PIN)
        }

        // D. Exam Cities
        checkRequired(FormSection.EXAM_CITY, Fields.CITY_1, record.examCityChoice1)
        checkRequired(FormSection.EXAM_CITY, Fields.CITY_2, record.examCityChoice2)
        checkRequired(FormSection.EXAM_CITY, Fields.CITY_3, record.examCityChoice3)

        // E. Parents Details
        checkRequired(FormSection.PARENTS, Fields.FATHER_QUAL, record.fatherQualification)
        checkRequired(FormSection.PARENTS, Fields.FATHER_OCC, record.fatherOccupation)
        checkRequired(FormSection.PARENTS, Fields.MOTHER_QUAL, record.motherQualification)
        checkRequired(FormSection.PARENTS, Fields.MOTHER_OCC, record.motherOccupation)

        // F. Address Details
        checkRequired(FormSection.ADDRESS, Fields.FULL_ADDRESS, record.fullAadhaarAddress)
        if (record.addressPinCode.isBlank()) {
            mark(FormSection.ADDRESS, Fields.ADDRESS_PIN, ValidationErrorType.REQUIRED)
        } else if (!isValidPin(record.addressPinCode)) {
            mark(FormSection.ADDRESS, Fields.ADDRESS_PIN, ValidationErrorType.INVALID_PIN)
        }

        // G. Required Documents (Category certificate is optional)
        checkRequired(FormSection.DOCUMENTS, Fields.DOC_PHOTO, record.photoPath)
        checkRequired(FormSection.DOCUMENTS, Fields.DOC_SIGNATURE, record.signaturePath)
        checkRequired(FormSection.DOCUMENTS, Fields.DOC_AADHAAR, record.aadhaarPhotoPath)
        checkRequired(FormSection.DOCUMENTS, Fields.DOC_TENTH, record.tenthMarksheetPath)
        checkRequired(FormSection.DOCUMENTS, Fields.DOC_TWELFTH, record.twelfthMarksheetPath)

        return ValidationResult(
            fieldErrors = errors,
            missingBySection = sectionCounts
        )
    }

    fun validateDraft(record: StudentRecord): ValidationResult {
        val errors = mutableMapOf<String, ValidationErrorType>()
        val sectionCounts = mutableMapOf<FormSection, Int>()

        fun mark(section: FormSection, field: String, error: ValidationErrorType) {
            errors[field] = error
            sectionCounts[section] = (sectionCounts[section] ?: 0) + 1
        }

        if (record.studentName.isBlank()) {
            mark(FormSection.PERSONAL, Fields.STUDENT_NAME, ValidationErrorType.REQUIRED)
        }
        if (record.aadhaarNumber.isNotBlank() && !isValidAadhaar(record.aadhaarNumber)) {
            mark(FormSection.PERSONAL, Fields.AADHAAR, ValidationErrorType.INVALID_AADHAAR)
        }
        if (record.studentMobile.isNotBlank() && !isValidMobile(record.studentMobile)) {
            mark(FormSection.PERSONAL, Fields.STUDENT_MOBILE, ValidationErrorType.INVALID_MOBILE)
        }
        if (record.emergencyMobile.isNotBlank() && !isValidMobile(record.emergencyMobile)) {
            mark(FormSection.PERSONAL, Fields.EMERGENCY_MOBILE, ValidationErrorType.INVALID_MOBILE)
        }
        if (record.studentEmail.isNotBlank() && !isValidEmail(record.studentEmail)) {
            mark(FormSection.PERSONAL, Fields.STUDENT_EMAIL, ValidationErrorType.INVALID_EMAIL)
        }
        if (record.emergencyEmail.isNotBlank() && !isValidEmail(record.emergencyEmail)) {
            mark(FormSection.PERSONAL, Fields.EMERGENCY_EMAIL, ValidationErrorType.INVALID_EMAIL)
        }
        if (record.dateOfBirth.isNotBlank() && !isValidDob(record.dateOfBirth)) {
            mark(FormSection.PERSONAL, Fields.DOB, ValidationErrorType.INVALID_DOB)
        }

        return ValidationResult(fieldErrors = errors, missingBySection = sectionCounts)
    }
}
