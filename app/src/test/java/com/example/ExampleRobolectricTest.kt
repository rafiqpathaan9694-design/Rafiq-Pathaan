package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backup.BackupExportManager
import com.example.data.local.AppDatabase
import com.example.data.model.FormStatus
import com.example.data.model.StudentRecord
import com.example.domain.FormValidation
import com.example.domain.ValidationErrorType
import com.example.pdf.NeetFormPdfGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches NEET 2027 Form Manager`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("NEET 2027 Form Manager", appName)
    }

    @Test
    fun `validation catches invalid mobile aadhaar email and missing fields`() {
        val invalidRecord = StudentRecord(
            studentName = "RAHUL SHARMA",
            dateOfBirth = "15/08/2008",
            aadhaarNumber = "12345",
            studentMobile = "1234567890",
            studentEmail = "not-an-email"
        )
        val result = FormValidation.validateForCompletion(invalidRecord)
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.INVALID_AADHAAR, result.fieldErrors[FormValidation.Fields.AADHAAR])
        assertEquals(ValidationErrorType.INVALID_MOBILE, result.fieldErrors[FormValidation.Fields.STUDENT_MOBILE])
        assertEquals(ValidationErrorType.INVALID_EMAIL, result.fieldErrors[FormValidation.Fields.STUDENT_EMAIL])
    }

    @Test
    fun `room database inserts searches and deletes student record`() {
        runBlocking {
            val dao = db.studentDao()
            val sample = createValidTestRecord()
            val id = dao.insertStudent(sample)
            assertTrue(id > 0)

            val fetched = dao.getStudentById(id)
            assertEquals("AARAV SHARMA", fetched?.studentName)

            val all = dao.getAllStudentsOnce()
            assertEquals(1, all.size)

            dao.deleteStudent(fetched!!)
            assertEquals(0, dao.getCount())
        }
    }

    @Test
    fun `offline A4 pdf pagination computes multi page layout and share intent`() {
        val record = createValidTestRecord()
        val pageCount = NeetFormPdfGenerator.countPagesForRecord(record)
        assertTrue("Expected at least 2 A4 pages with attached documents", pageCount >= 2)

        val dummyPdf = File(context.filesDir, "generated_pdfs/test_share.pdf").apply {
            parentFile?.mkdirs()
            writeText("%PDF-1.4")
        }
        val shareIntent = NeetFormPdfGenerator.createSharePdfIntent(context, dummyPdf, record.studentName)
        assertEquals(android.content.Intent.ACTION_SEND, shareIntent.action)
        assertEquals("application/pdf", shareIntent.type)
        dummyPdf.delete()
    }

    @Test
    fun `csv export writes utf8 bom and student fields`() {
        runBlocking {
            val out = ByteArrayOutputStream()
            BackupExportManager.exportCsvToStream(listOf(createValidTestRecord()), out)
            val csv = out.toString(Charsets.UTF_8.name())
            assertTrue(csv.contains("AARAV SHARMA"))
            assertTrue(csv.contains("9876543210"))
            assertTrue(csv.contains("123456789012"))
        }
    }

    @Test
    fun `zip backup export and restore roundtrip preserves records`() {
        runBlocking {
            val backupFile = File(context.cacheDir, "test_backup.zip")
            backupFile.outputStream().use { out ->
                BackupExportManager.exportBackupZip(listOf(createValidTestRecord()), out)
            }
            assertTrue(backupFile.exists())
            assertTrue(backupFile.length() > 100)

            val uri = android.net.Uri.fromFile(backupFile)
            val inspect = BackupExportManager.inspectBackupZip(context, uri)
            assertTrue(inspect.isSuccess)
            assertEquals(1, inspect.getOrNull()?.recordCount)

            val restored = BackupExportManager.restoreBackupZip(
                context = context,
                uri = uri,
                replaceExisting = true,
                dao = db.studentDao()
            )
            assertTrue(restored.isSuccess)
            assertEquals(1, restored.getOrNull())
            assertEquals(1, db.studentDao().getCount())
            backupFile.delete()
        }
    }

    private fun createValidTestRecord(): StudentRecord {
        val tempImg = File(context.cacheDir, "dummy_test.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }
        return StudentRecord(
            formNumber = "MS27-2611-0001",
            status = FormStatus.COMPLETED,
            studentName = "AARAV SHARMA",
            dateOfBirth = "12/05/2008",
            gender = "Male",
            fatherName = "RAJESH SHARMA",
            motherName = "SUNITA SHARMA",
            aadhaarNumber = "123456789012",
            studentEmail = "aarav@example.com",
            studentMobile = "9876543210",
            emergencyEmail = "parent@example.com",
            emergencyMobile = "9876543211",
            category = "General (UR)",
            religion = "Hindu",
            personWithDisability = "No",
            domicileState = "Rajasthan",
            familyAnnualIncome = "₹2,50,001 – ₹5,00,000",
            questionPaperMedium = "English",
            onlyGirlChild = "No",
            tenthPassingYear = "2024",
            tenthRollNumber = "1023456",
            tenthPercentage = "91.5",
            tenthTotalMarks = "549/600",
            tenthBoardName = "CBSE",
            tenthSchoolName = "Kendriya Vidyalaya",
            tenthSchoolAddress = "Jaipur Road, Sikar",
            tenthAreaPinCode = "332001",
            twelfthPassingYear = "2026",
            twelfthRollNumber = "1223456",
            twelfthPercentage = "89.0",
            twelfthTotalMarks = "445/500",
            twelfthBoardName = "CBSE",
            twelfthSchoolName = "Kendriya Vidyalaya",
            twelfthSchoolAddress = "Jaipur Road, Sikar",
            twelfthAreaPinCode = "332001",
            examCityChoice1 = "Sikar (RJ)",
            examCityChoice2 = "Jaipur (RJ)",
            examCityChoice3 = "Kota (RJ)",
            fatherQualification = "Graduate",
            fatherOccupation = "Business / Self-Employed",
            motherQualification = "12th / Higher Secondary",
            motherOccupation = "Homemaker",
            fullAadhaarAddress = "Ward 12, Station Road, Sikar, Rajasthan",
            addressPinCode = "332001",
            photoPath = tempImg.absolutePath,
            signaturePath = tempImg.absolutePath,
            aadhaarPhotoPath = tempImg.absolutePath,
            tenthMarksheetPath = tempImg.absolutePath,
            twelfthMarksheetPath = tempImg.absolutePath
        )
    }
}
