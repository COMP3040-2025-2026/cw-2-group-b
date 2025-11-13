package com.nottingham.mynottingham.data.repository

import com.nottingham.mynottingham.data.mapper.CourseMapper
import com.nottingham.mynottingham.data.model.Course
import com.nottingham.mynottingham.data.model.StudentAttendance
import com.nottingham.mynottingham.data.remote.RetrofitInstance
import com.nottingham.mynottingham.data.remote.dto.MarkAttendanceRequest
import com.nottingham.mynottingham.data.remote.dto.SignInRequest
import com.nottingham.mynottingham.data.remote.dto.UnlockSessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstattRepository {

    private val apiService = RetrofitInstance.apiService

    suspend fun getTeacherCourses(teacherId: Long, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTeacherCourses(teacherId, date)
                if (response.isSuccessful && response.body()?.success == true) {
                    val courses = response.body()?.data?.map { CourseMapper.toCourse(it) } ?: emptyList()
                    Result.success(courses)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to load courses"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStudentCourses(studentId: Long, date: String): Result<List<Course>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStudentCourses(studentId, date)
                if (response.isSuccessful && response.body()?.success == true) {
                    val courses = response.body()?.data?.map { CourseMapper.toCourse(it) } ?: emptyList()
                    Result.success(courses)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to load courses"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun unlockSession(teacherId: Long, courseScheduleId: Long, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UnlockSessionRequest(courseScheduleId, date)
                val response = apiService.unlockSession(teacherId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to unlock session"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun lockSession(teacherId: Long, courseScheduleId: Long, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UnlockSessionRequest(courseScheduleId, date)
                val response = apiService.lockSession(teacherId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to lock session"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun signIn(studentId: Long, courseScheduleId: Long, date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SignInRequest(courseScheduleId, date)
                val response = apiService.signIn(studentId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to sign in"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStudentAttendanceList(
        teacherId: Long,
        courseScheduleId: Long,
        date: String
    ): Result<List<StudentAttendance>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStudentAttendanceList(teacherId, courseScheduleId, date)
                if (response.isSuccessful && response.body()?.success == true) {
                    val students = response.body()?.data?.map { CourseMapper.toStudentAttendance(it) } ?: emptyList()
                    Result.success(students)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to load student list"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun markAttendance(
        teacherId: Long,
        studentId: Long,
        courseScheduleId: Long,
        date: String,
        status: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = MarkAttendanceRequest(studentId, courseScheduleId, date, status)
                val response = apiService.markAttendance(teacherId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to mark attendance"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
