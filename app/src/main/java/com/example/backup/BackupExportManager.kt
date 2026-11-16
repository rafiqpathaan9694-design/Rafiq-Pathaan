package com.example.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.StudentDao
import com.example.data.model.StudentRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupInspectResult(
    val recordCount: Int,
    val exportedAt: String
)

object BackupExportManager {

    private const val STUDENTS_JSON_ENTRY = "students.json"
    private const val DOCS_DIR_PREFIX = "docs/"

    suspend fun exportCsvToStream(
        students: List<StudentRecord>,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel compatibility
        sb.append('\uFEFF')

        val headers = listOf(
            "Form No", "Status", "Student Name", "Date of Birth", "Gender",
            "Father's Name", "Mother's Name", "Aadhaar Number", "Student Mobile", "Student Email",
            "Emergency Mobile", "Emergency Email", "Category", "Religion", "PwD",
            "Category Cert Status", "Category Cert ID", "Issuing Authority", "Cert Issue Date",
            "Domicile State", "Annual Income", "Question Medium", "Only Girl Child",
            "10th Year", "10th Roll No", "10th %", "10th Total Marks", "10th Board", "10th School", "10th School Address", "10th PIN",
            "12th Year", "12th Roll No", "12th %", "12th Total Marks", "12th Board", "12th School", "12th School Address", "12th PIN",
            "Exam City 1", "Exam City 2", "Exam City 3",
            "Father Qualification", "Father Occupation", "Mother Qualification", "Mother Occupation",
            "Full Aadhaar Address", "Address PIN",
            "Photo Attached", "Signature Attached", "Aadhaar Attached", "10th Marksheet Attached", "12th Marksheet Attached", "Category Cert Attached"
        )
        sb.append(headers.joinToString(",") { csvEscape(it) }).append("\r\n")

        for (s in students) {
            val row = listOf(
                s.formNumber, s.status, s.studentName, s.dateOfBirth, s.gender,
                s.fatherName, s.motherName, s.aadhaarNumber, s.studentMobile, s.studentEmail,
                s.emergencyMobile, s.emergencyEmail, s.category, s.religion, s.personWithDisability,
                s.categoryCertificate, s.categoryCertificateId, s.issuingAuthorityName, s.certificateIssueDate,
                s.domicileState, s.familyAnnualIncome, s.questionPaperMedium, s.onlyGirlChild,
                s.tenthPassingYear, s.tenthRollNumber, s.tenthPercentage, s.tenthTotalMarks,
                s.tenthBoardName, s.tenthSchoolName, s.tenthSchoolAddress, s.tenthAreaPinCode,
                s.twelfthPassingYear, s.twelfthRollNumber, s.twelfthPercentage, s.twelfthTotalMarks,
                s.twelfthBoardName, s.twelfthSchoolName, s.twelfthSchoolAddress, s.twelfthAreaPinCode,
                s.examCityChoice1, s.examCityChoice2, s.examCityChoice3,
                s.fatherQualification, s.fatherOccupation, s.motherQualification, s.motherOccupation,
                s.fullAadhaarAddress, s.addressPinCode,
                yesNoFile(s.photoPath), yesNoFile(s.signaturePath), yesNoFile(s.aadhaarPhotoPath),
                yesNoFile(s.tenthMarksheetPath), yesNoFile(s.twelfthMarksheetPath), yesNoFile(s.categoryCertificatePath)
            )
            sb.append(row.joinToString(",") { csvEscape(it) }).append("\r\n")
        }

        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(sb.toString())
        }
    }

    suspend fun createShareableCsvFile(context: Context, students: List<StudentRecord>): Intent =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared_exports").apply { if (!exists()) mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
            val csvFile = File(dir, "MobileSolution_NEET2027_$stamp.csv")
            FileOutputStream(csvFile).use { exportCsvToStream(students, it) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Mobile Solution NEET 2027 Student Records CSV")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

    suspend fun exportBackupZip(
        students: List<StudentRecord>,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val docsToPack = mutableMapOf<String, File>()
        val jsonArray = JSONArray()

        for (s in students) {
            val obj = studentToJson(s) { path ->
                if (path.isBlank()) {
                    ""
                } else {
                    val file = File(path)
                    if (file.exists()) {
                        val entryName = "${DOCS_DIR_PREFIX}${file.name}"
                        docsToPack[entryName] = file
                        entryName
                    } else {
                        ""
                    }
                }
            }
            jsonArray.put(obj)
        }

        val root = JSONObject().apply {
            put("app", "MOBILE_SOLUTION_NEET_2027")
            put("version", 1)
            put("exportedAt", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date()))
            put("students", jsonArray)
        }

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(STUDENTS_JSON_ENTRY))
            zipOut.write(root.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            for ((entryName, file) in docsToPack) {
                zipOut.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { input -> input.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }

    suspend fun inspectBackupZip(context: Context, uri: Uri): Result<BackupInspectResult> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    var jsonText: String? = null
                    ZipInputStream(BufferedInputStream(stream)).use { zipIn ->
                        var entry = zipIn.nextEntry
                        while (entry != null) {
                            if (entry.name == STUDENTS_JSON_ENTRY) {
                                jsonText = readStreamText(zipIn)
                                break
                            }
                            entry = zipIn.nextEntry
                        }
                    }
                    val raw = jsonText
                        ?: return@withContext Result.failure(IllegalArgumentException("Invalid backup file (missing students.json)"))
                    val root = JSONObject(raw)
                    val arr = root.optJSONArray("students")
                        ?: return@withContext Result.failure(IllegalArgumentException("Invalid backup format"))
                    val exportedAt = root.optString("exportedAt", "-")
                    Result.success(BackupInspectResult(arr.length(), exportedAt))
                } ?: Result.failure(IllegalStateException("Cannot read selected file"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun restoreBackupZip(
        context: Context,
        uri: Uri,
        replaceExisting: Boolean,
        dao: StudentDao
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val docsDir = File(context.filesDir, "student_docs").apply { if (!exists()) mkdirs() }
            val docsCanonical = docsDir.canonicalPath
            val restoredFiles = mutableMapOf<String, String>()
            var jsonText: String? = null

            context.contentResolver.openInputStream(uri)?.use { rawStream ->
                ZipInputStream(BufferedInputStream(rawStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            if (name == STUDENTS_JSON_ENTRY) {
                                jsonText = readStreamText(zipIn)
                            } else if (name.startsWith(DOCS_DIR_PREFIX)) {
                                val fileName = File(name).name
                                if (fileName.isNotBlank()) {
                                    val uniqueName = "restored_${UUID.randomUUID().toString().take(8)}_$fileName"
                                    val outFile = File(docsDir, uniqueName)
                                    if (outFile.canonicalPath.startsWith(docsCanonical)) {
                                        FileOutputStream(outFile).use { out -> zipIn.copyTo(out) }
                                        restoredFiles[name] = outFile.absolutePath
                                    }
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open backup file"))

            val rawJson = jsonText
                ?: return@withContext Result.failure(IllegalArgumentException("Missing students.json in backup"))
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("students")

            if (replaceExisting) {
                val existing = dao.getAllStudentsOnce()
                existing.flatMap { it.allDocumentPaths() }.forEach { path ->
                    val f = File(path)
                    if (f.exists() && f.canonicalPath.startsWith(docsCanonical)) f.delete()
                }
                dao.deleteAllStudents()
            }

            var count = 0
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val record = jsonToStudent(obj) { entryName ->
                    restoredFiles[entryName] ?: ""
                }.copy(id = 0L)
                dao.insertStudent(record)
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readStreamText(input: InputStream): String {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var len: Int
        while (input.read(buffer).also { len = it } != -1) {
            baos.write(buffer, 0, len)
        }
        return baos.toString(Charsets.UTF_8.name())
    }

    private fun csvEscape(value: String): String {
        val cleaned = value.replace("\r\n", " ").replace("\n", " ")
        return "\"" + cleaned.replace("\"", "\"\"") + "\""
    }

    private fun yesNoFile(path: String): String =
        if (path.isNotBlank() && File(path).exists()) "Yes" else "No"

    private fun studentToJson(s: StudentRecord, mapPath: (String) -> String): JSONObject {
        return JSONObject().apply {
            put("formNumber", s.formNumber)
            put("status", s.status)
            put("createdAt", s.createdAt)
            put("updatedAt", s.updatedAt)
            put("studentName", s.studentName)
            put("dateOfBirth", s.dateOfBirth)
            put("gender", s.gender)
            put("fatherName", s.fatherName)
            put("motherName", s.motherName)
            put("aadhaarNumber", s.aadhaarNumber)
            put("studentEmail", s.studentEmail)
            put("studentMobile", s.studentMobile)
            put("emergencyEmail", s.emergencyEmail)
            put("emergencyMobile", s.emergencyMobile)
            put("category", s.category)
            put("religion", s.religion)
            put("personWithDisability", s.personWithDisability)
            put("categoryCertificateId", s.categoryCertificateId)
            put("categoryCertificate", s.categoryCertificate)
            put("issuingAuthorityName", s.issuingAuthorityName)
            put("certificateIssueDate", s.certificateIssueDate)
            put("domicileState", s.domicileState)
            put("familyAnnualIncome", s.familyAnnualIncome)
            put("questionPaperMedium", s.questionPaperMedium)
            put("onlyGirlChild", s.onlyGirlChild)
            put("tenthPassingYear", s.tenthPassingYear)
            put("tenthRollNumber", s.tenthRollNumber)
            put("tenthPercentage", s.tenthPercentage)
            put("tenthTotalMarks", s.tenthTotalMarks)
            put("tenthBoardName", s.tenthBoardName)
            put("tenthSchoolName", s.tenthSchoolName)
            put("tenthSchoolAddress", s.tenthSchoolAddress)
            put("tenthAreaPinCode", s.tenthAreaPinCode)
            put("twelfthPassingYear", s.twelfthPassingYear)
            put("twelfthRollNumber", s.twelfthRollNumber)
            put("twelfthPercentage", s.twelfthPercentage)
            put("twelfthTotalMarks", s.twelfthTotalMarks)
            put("twelfthBoardName", s.twelfthBoardName)
            put("twelfthSchoolName", s.twelfthSchoolName)
            put("twelfthSchoolAddress", s.twelfthSchoolAddress)
            put("twelfthAreaPinCode", s.twelfthAreaPinCode)
            put("examCityChoice1", s.examCityChoice1)
            put("examCityChoice2", s.examCityChoice2)
            put("examCityChoice3", s.examCityChoice3)
            put("fatherQualification", s.fatherQualification)
            put("fatherOccupation", s.fatherOccupation)
            put("motherQualification", s.motherQualification)
            put("motherOccupation", s.motherOccupation)
            put("fullAadhaarAddress", s.fullAadhaarAddress)
            put("addressPinCode", s.addressPinCode)
            put("photoPath", mapPath(s.photoPath))
            put("signaturePath", mapPath(s.signaturePath))
            put("aadhaarPhotoPath", mapPath(s.aadhaarPhotoPath))
            put("tenthMarksheetPath", mapPath(s.tenthMarksheetPath))
            put("twelfthMarksheetPath", mapPath(s.twelfthMarksheetPath))
            put("categoryCertificatePath", mapPath(s.categoryCertificatePath))
        }
    }

    private fun jsonToStudent(o: JSONObject, resolveDoc: (String) -> String): StudentRecord {
        return StudentRecord(
            formNumber = o.optString("formNumber", ""),
            status = o.optString("status", "PENDING"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            studentName = o.optString("studentName", ""),
            dateOfBirth = o.optString("dateOfBirth", ""),
            gender = o.optString("gender", ""),
            fatherName = o.optString("fatherName", ""),
            motherName = o.optString("motherName", ""),
            aadhaarNumber = o.optString("aadhaarNumber", ""),
            studentEmail = o.optString("studentEmail", ""),
            studentMobile = o.optString("studentMobile", ""),
            emergencyEmail = o.optString("emergencyEmail", ""),
            emergencyMobile = o.optString("emergencyMobile", ""),
            category = o.optString("category", ""),
            religion = o.optString("religion", ""),
            personWithDisability = o.optString("personWithDisability", "No"),
            categoryCertificateId = o.optString("categoryCertificateId", ""),
            categoryCertificate = o.optString("categoryCertificate", ""),
            issuingAuthorityName = o.optString("issuingAuthorityName", ""),
            certificateIssueDate = o.optString("certificateIssueDate", ""),
            domicileState = o.optString("domicileState", ""),
            familyAnnualIncome = o.optString("familyAnnualIncome", ""),
            questionPaperMedium = o.optString("questionPaperMedium", "English"),
            onlyGirlChild = o.optString("onlyGirlChild", "No"),
            tenthPassingYear = o.optString("tenthPassingYear", ""),
            tenthRollNumber = o.optString("tenthRollNumber", ""),
            tenthPercentage = o.optString("tenthPercentage", ""),
            tenthTotalMarks = o.optString("tenthTotalMarks", ""),
            tenthBoardName = o.optString("tenthBoardName", ""),
            tenthSchoolName = o.optString("tenthSchoolName", ""),
            tenthSchoolAddress = o.optString("tenthSchoolAddress", ""),
            tenthAreaPinCode = o.optString("tenthAreaPinCode", ""),
            twelfthPassingYear = o.optString("twelfthPassingYear", ""),
            twelfthRollNumber = o.optString("twelfthRollNumber", ""),
            twelfthPercentage = o.optString("twelfthPercentage", ""),
            twelfthTotalMarks = o.optString("twelfthTotalMarks", ""),
            twelfthBoardName = o.optString("twelfthBoardName", ""),
            twelfthSchoolName = o.optString("twelfthSchoolName", ""),
            twelfthSchoolAddress = o.optString("twelfthSchoolAddress", ""),
            twelfthAreaPinCode = o.optString("twelfthAreaPinCode", ""),
            examCityChoice1 = o.optString("examCityChoice1", ""),
            examCityChoice2 = o.optString("examCityChoice2", ""),
            examCityChoice3 = o.optString("examCityChoice3", ""),
            fatherQualification = o.optString("fatherQualification", ""),
            fatherOccupation = o.optString("fatherOccupation", ""),
            motherQualification = o.optString("motherQualification", ""),
            motherOccupation = o.optString("motherOccupation", ""),
            fullAadhaarAddress = o.optString("fullAadhaarAddress", ""),
            addressPinCode = o.optString("addressPinCode", ""),
            photoPath = resolveDoc(o.optString("photoPath", "")),
            signaturePath = resolveDoc(o.optString("signaturePath", "")),
            aadhaarPhotoPath = resolveDoc(o.optString("aadhaarPhotoPath", "")),
            tenthMarksheetPath = resolveDoc(o.optString("tenthMarksheetPath", "")),
            twelfthMarksheetPath = resolveDoc(o.optString("twelfthMarksheetPath", "")),
            categoryCertificatePath = resolveDoc(o.optString("categoryCertificatePath", ""))
        )
    }
}
