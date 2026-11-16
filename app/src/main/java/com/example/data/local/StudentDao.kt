package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StudentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM student_records ORDER BY updatedAt DESC")
    fun observeAllStudents(): Flow<List<StudentRecord>>

    @Query(
        """
        SELECT * FROM student_records
        WHERE studentName LIKE '%' || :query || '%'
           OR studentMobile LIKE '%' || :query || '%'
           OR emergencyMobile LIKE '%' || :query || '%'
           OR aadhaarNumber LIKE '%' || :query || '%'
           OR formNumber LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchStudents(query: String): Flow<List<StudentRecord>>

    @Query("SELECT * FROM student_records WHERE id = :id")
    fun observeStudentById(id: Long): Flow<StudentRecord?>

    @Query("SELECT * FROM student_records WHERE id = :id")
    suspend fun getStudentById(id: Long): StudentRecord?

    @Query("SELECT * FROM student_records ORDER BY updatedAt DESC")
    suspend fun getAllStudentsOnce(): List<StudentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentRecord): Long

    @Update
    suspend fun updateStudent(student: StudentRecord)

    @Delete
    suspend fun deleteStudent(student: StudentRecord)

    @Query("DELETE FROM student_records")
    suspend fun deleteAllStudents()

    @Query("SELECT COUNT(*) FROM student_records")
    suspend fun getCount(): Int
}
