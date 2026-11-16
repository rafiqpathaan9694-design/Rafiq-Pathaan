package com.example.data.repository

import com.example.data.local.DocumentStorageManager
import com.example.data.local.StudentDao
import com.example.data.model.StudentRecord
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentRepository(
    private val studentDao: StudentDao,
    private val documentStorage: DocumentStorageManager
) {
    val allStudents: Flow<List<StudentRecord>> = studentDao.observeAllStudents()

    fun searchStudents(query: String): Flow<List<StudentRecord>> {
        return if (query.isBlank()) {
            studentDao.observeAllStudents()
        } else {
            studentDao.searchStudents(query.trim())
        }
    }

    fun observeStudentById(id: Long): Flow<StudentRecord?> = studentDao.observeStudentById(id)

    suspend fun getStudentById(id: Long): StudentRecord? = studentDao.getStudentById(id)

    suspend fun getAllStudentsOnce(): List<StudentRecord> = studentDao.getAllStudentsOnce()

    suspend fun saveStudent(record: StudentRecord): Long {
        val now = System.currentTimeMillis()
        val normalized = record.copy(
            studentName = record.studentName.trim().uppercase(Locale.ENGLISH),
            updatedAt = now
        )
        return if (normalized.id == 0L) {
            val count = studentDao.getCount() + 1
            val formNo = normalized.formNumber.ifBlank { generateFormNumber(count) }
            val insertedId = studentDao.insertStudent(
                normalized.copy(
                    formNumber = formNo,
                    createdAt = now
                )
            )
            insertedId
        } else {
            val formNo = normalized.formNumber.ifBlank { generateFormNumber(normalized.id.toInt()) }
            studentDao.updateStudent(normalized.copy(formNumber = formNo))
            normalized.id
        }
    }

    suspend fun deleteStudent(record: StudentRecord) {
        record.allDocumentPaths().forEach { documentStorage.deleteFileIfExists(it) }
        studentDao.deleteStudent(record)
    }

    private fun generateFormNumber(sequence: Int): String {
        val datePart = SimpleDateFormat("yyMM", Locale.ENGLISH).format(Date())
        return "MS27-$datePart-%04d".format(sequence)
    }
}
