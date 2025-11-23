package com.nottingham.mynottingham.data.remote.api

import com.nottingham.mynottingham.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

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

    @PUT("users/{id}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Long,
        @Body user: UserUpdateRequest
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

    @GET("errand/{id}")
    suspend fun getErrandById(
        @Header("Authorization") token: String,
        @Path("id") errandId: String
    ): Response<ErrandResponse>

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

    @POST("errand/{errandId}/drop")
    suspend fun dropErrand(
        @Header("Authorization") token: String,
        @Path("errandId") errandId: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ErrandResponse>

    @PUT("errand/{errandId}/status")
    suspend fun updateErrandStatus(
        @Header("Authorization") token: String,
        @Path("errandId") errandId: String,
        @Body request: UpdateStatusRequest
    ): Response<ErrandResponse>

    @DELETE("errand/{errandId}")
    suspend fun deleteErrand(
        @Header("Authorization") token: String,
        @Path("errandId") errandId: String
    ): Response<Unit>

    @PUT("errand/{errandId}")
    suspend fun updateErrand(
        @Header("Authorization") token: String,
        @Path("errandId") errandId: String,
        @Body request: CreateErrandRequest
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

    // ========== Messaging ==========

    @GET("message/conversations")
    suspend fun getConversations(
        @Header("Authorization") token: String
    ): Response<ConversationListResponse>

    @GET("message/conversations/{conversationId}")
    suspend fun getConversation(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String
    ): Response<ConversationResponse>

    @GET("message/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<MessageListResponse>

    @POST("message/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>

    @DELETE("message/messages/{messageId}")
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Path("messageId") messageId: Long
    ): Response<ApiResponse<Any>>

    @POST("message/conversations")
    suspend fun createConversation(
        @Header("Authorization") token: String,
        @Body request: CreateConversationRequest
    ): Response<ConversationResponse>

    @PUT("message/conversations/{conversationId}/pin")
    suspend fun updatePinnedStatus(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String,
        @Body request: UpdatePinnedStatusRequest
    ): Response<ConversationResponse>

    @POST("message/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String
    ): Response<ApiResponse<Any>>

    @POST("message/typing")
    suspend fun updateTypingStatus(
        @Header("Authorization") token: String,
        @Body request: TypingStatusRequest
    ): Response<ApiResponse<Any>>

    @GET("message/search")
    suspend fun searchMessages(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): Response<MessageListResponse>

    @GET("message/contacts/suggestions")
    suspend fun getContactSuggestions(
        @Header("Authorization") token: String
    ): Response<ContactSuggestionResponse>

    @GET("message/contacts/default/{userId}")
    suspend fun getDefaultContacts(
        @Path("userId") userId: String
    ): Response<ContactSuggestionResponse>

    @DELETE("message/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String
    ): Response<ApiResponse<Any>>

    // ========== Forum ==========

    @GET("forum/posts")
    suspend fun getForumPosts(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null
    ): Response<PagedForumPostsResponse>

    @GET("forum/posts/{id}")
    suspend fun getForumPostById(
        @Header("Authorization") token: String,
        @Path("id") postId: Long
    ): Response<ForumPostDetailResponse>

    @Multipart
    @POST("forum/posts")
    suspend fun createForumPost(
        @Header("Authorization") token: String,
        @Part("post") request: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<ForumPostResponse>

    @PUT("forum/posts/{id}")
    suspend fun updateForumPost(
        @Header("Authorization") token: String,
        @Path("id") postId: Long,
        @Body request: UpdateForumPostRequest
    ): Response<ForumPostResponse>

    @DELETE("forum/posts/{id}")
    suspend fun deleteForumPost(
        @Header("Authorization") token: String,
        @Path("id") postId: Long
    ): Response<ApiResponse<Any>>

    @POST("forum/posts/{id}/like")
    suspend fun likeForumPost(
        @Header("Authorization") token: String,
        @Path("id") postId: Long
    ): Response<ForumPostResponse>

    @POST("forum/posts/{id}/comments")
    suspend fun createForumComment(
        @Header("Authorization") token: String,
        @Path("id") postId: Long,
        @Body request: CreateCommentRequest
    ): Response<ForumCommentResponse>

    @POST("forum/comments/{id}/like")
    suspend fun likeForumComment(
        @Header("Authorization") token: String,
        @Path("id") commentId: Long
    ): Response<ForumCommentResponse>
}