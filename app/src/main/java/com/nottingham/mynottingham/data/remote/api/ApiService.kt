package com.nottingham.mynottingham.data.remote.api

import com.nottingham.mynottingham.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Main API service interface for My Nottingham
 */
interface ApiService {

    // ========== Authentication ==========

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    @GET("user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserDto>>

    // ========== Shuttle ==========

    @GET("shuttle/routes")
    suspend fun getShuttleRoutes(): Response<List<ShuttleResponse>>

    @GET("shuttle/schedule/{route}")
    suspend fun getShuttleSchedule(
        @Path("route") route: String
    ): Response<ShuttleScheduleResponse>

    // ========== Booking ==========

    @GET("booking/facilities")
    suspend fun getFacilities(): Response<List<FacilityResponse>>

    @GET("booking/slots/{facilityId}")
    suspend fun getAvailableSlots(
        @Path("facilityId") facilityId: String,
        @Query("date") date: String
    ): Response<List<TimeSlotResponse>>

    @POST("booking/create")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body request: CreateBookingRequest
    ): Response<BookingResponse>

    @GET("booking/user")
    suspend fun getUserBookings(
        @Header("Authorization") token: String
    ): Response<List<BookingResponse>>

    @DELETE("booking/{bookingId}")
    suspend fun cancelBooking(
        @Header("Authorization") token: String,
        @Path("bookingId") bookingId: String
    ): Response<Unit>

    // ========== Errand ==========

    @GET("errand/available")
    suspend fun getAvailableErrands(): Response<List<ErrandResponse>>

    @POST("errand/create")
    suspend fun createErrand(
        @Header("Authorization") token: String,
        @Body request: CreateErrandRequest
    ): Response<ErrandResponse>

    @POST("errand/{errandId}/accept")
    suspend fun acceptErrand(
        @Header("Authorization") token: String,
        @Path("errandId") errandId: String
    ): Response<ErrandResponse>

    @PUT("errand/{errandId}/status")
    suspend fun updateErrandStatus(
        @Header("Authorization") token: String,
        @Path("errandId") errandId: String,
        @Body request: UpdateStatusRequest
    ): Response<ErrandResponse>

    // ========== Attendance (Instatt) ==========

    @GET("system/time")
    suspend fun getSystemTime(): Response<ApiResponse<SystemTimeDto>>

    @GET("attendance/teacher/{teacherId}/courses")
    suspend fun getTeacherCourses(
        @Path("teacherId") teacherId: Long,
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<CourseScheduleDto>>>

    @GET("attendance/student/{studentId}/courses")
    suspend fun getStudentCourses(
        @Path("studentId") studentId: Long,
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<CourseScheduleDto>>>

    @POST("attendance/teacher/{teacherId}/unlock")
    suspend fun unlockSession(
        @Path("teacherId") teacherId: Long,
        @Body request: UnlockSessionRequest
    ): Response<ApiResponse<Any>>

    @POST("attendance/teacher/{teacherId}/lock")
    suspend fun lockSession(
        @Path("teacherId") teacherId: Long,
        @Body request: UnlockSessionRequest
    ): Response<ApiResponse<Any>>

    @POST("attendance/student/{studentId}/signin")
    suspend fun signIn(
        @Path("studentId") studentId: Long,
        @Body request: SignInRequest
    ): Response<ApiResponse<Any>>

    @GET("attendance/teacher/{teacherId}/course/{courseScheduleId}/students")
    suspend fun getStudentAttendanceList(
        @Path("teacherId") teacherId: Long,
        @Path("courseScheduleId") courseScheduleId: Long,
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<StudentAttendanceDto>>>

    @POST("attendance/teacher/{teacherId}/mark")
    suspend fun markAttendance(
        @Path("teacherId") teacherId: Long,
        @Body request: MarkAttendanceRequest
    ): Response<ApiResponse<Any>>
}
