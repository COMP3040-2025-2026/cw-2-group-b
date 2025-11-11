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
}
