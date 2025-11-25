package com.nottingham.mynottingham.ui.notti

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nottingham.mynottingham.data.local.NottiChatStorage
import com.nottingham.mynottingham.data.model.DayType
import com.nottingham.mynottingham.data.model.NottiCardData
import com.nottingham.mynottingham.data.model.NottiCardItem
import com.nottingham.mynottingham.data.model.NottiMessage
import com.nottingham.mynottingham.data.model.NottiMessageType
import com.nottingham.mynottingham.data.model.RouteSchedule
import com.nottingham.mynottingham.data.model.ShuttleRoute
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume

/**
 * NottiViewModel - Notti AI Assistant 的 ViewModel
 *
 * 使用 Firebase AI Logic (Gemini) 实现 AI 对话功能
 * 免费版本使用 Gemini Developer API
 * 聊天记录按用户账号本地缓存
 */
class NottiViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "NottiViewModel"

        // Base system prompt - 指示 AI 返回 JSON 格式
        private const val BASE_SYSTEM_PROMPT = """
You are Notti, a friendly AI assistant for University of Nottingham Malaysia students.

CRITICAL: You MUST ALWAYS respond in valid JSON format with this exact structure:
{
  "type": "text" or "shuttle" or "booking",
  "message": "Your helpful response here",
  "cardData": null or card object
}

TYPE CLASSIFICATION RULES:
- Use "shuttle" when: user asks about shuttle bus, routes, transport times, campus bus, going to Kajang/TBS/TTS/IOI, bus schedule
- Use "booking" when: user asks about sports facility, booking courts, basketball/badminton/tennis/squash/football, availability, reservations
- Use "text" for: everything else (greetings, general questions, campus info, events, help)

CARD DATA FORMAT:
When type is "shuttle", include cardData with shuttle schedule from CONTEXT DATA:
{
  "title": "Shuttle Schedule",
  "subtitle": "Today's date and day type",
  "items": [{"icon": "A", "label": "UNM ↔ TBS", "value": "→ 6:45pm"}, ...]
}

When type is "booking", include cardData with booking info:
{
  "title": "Sports Facility",
  "subtitle": "Availability info",
  "items": [{"label": "Facility", "value": "Time info"}, ...]
}

When type is "text", set cardData to null.

RESPONSE GUIDELINES:
- Keep message concise and helpful
- Use emojis occasionally to be friendly 😊
- For shuttle: summarize key times, don't repeat all data (card shows details)
- For booking: explain how to book, mention the "Book Now" button
- ALWAYS use the REAL DATA from the context provided

CAMPUS INFO:
- University of Nottingham Malaysia, Semenyih, Selangor
- Main buildings: Teaching Block, Admin Building, Student Association Building
- Sports facilities: Basketball, Badminton, Tennis, Squash, Football

REMEMBER: Your entire response must be valid JSON. No text outside the JSON object.
"""
    }

    // 聊天消息列表
    private val _messages = MutableLiveData<List<NottiMessage>>(emptyList())
    val messages: LiveData<List<NottiMessage>> = _messages

    // 是否正在等待 AI 响应
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 本地聊天记录存储
    private val chatStorage = NottiChatStorage(application)
    private val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Firebase Database 引用
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val bookingsRef = database.getReference("bookings")

    // 初始化 Gemini 模型 - 使用免费的 Gemini Developer API
    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.0-flash",
                systemInstruction = com.google.firebase.ai.type.content { text(BASE_SYSTEM_PROMPT) }
            )
    }

    // 对话历史（用于保持上下文）
    private val chat by lazy {
        generativeModel.startChat()
    }

    // 校车时刻表数据
    private val shuttleRoutes: List<ShuttleRoute> by lazy { loadShuttleRoutes() }

    /**
     * AI 响应的数据类
     */
    private data class NottiAIResponse(
        val type: String,  // "text", "shuttle", "booking"
        val message: String,
        val cardData: NottiCardData?
    )

    init {
        // 加载本地缓存的聊天记录
        loadChatHistory()
    }

    /**
     * 从本地存储加载聊天记录
     */
    private fun loadChatHistory() {
        val savedMessages = chatStorage.loadChatHistory(currentUserId)
        if (savedMessages.isNotEmpty()) {
            _messages.value = savedMessages
        } else {
            // 没有历史记录，显示欢迎消息
            addMessage(
                NottiMessage(
                    content = "Hi! I'm Notti, your AI campus assistant. How can I help you today?",
                    isFromUser = false
                )
            )
        }
    }

    /**
     * 保存聊天记录到本地存储
     */
    private fun saveChatHistory() {
        val currentMessages = _messages.value ?: return
        chatStorage.saveChatHistory(currentUserId, currentMessages)
    }

    // 消息计数器，确保每条消息有唯一 ID
    private var messageCounter = 0L

    /**
     * 生成唯一消息 ID
     */
    private fun generateMessageId(): String {
        return "${System.currentTimeMillis()}_${messageCounter++}"
    }

    /**
     * 发送消息给 AI 并获取响应
     */
    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // 提前生成 loading 消息 ID，以便在 catch 中使用
        val loadingMessageId = generateMessageId()

        viewModelScope.launch {
            try {
                // 添加用户消息 - 使用唯一 ID
                val userNottiMessage = NottiMessage(
                    id = generateMessageId(),
                    content = userMessage.trim(),
                    isFromUser = true
                )
                addMessage(userNottiMessage)

                // 添加加载中的 AI 消息
                val loadingMessage = NottiMessage(
                    id = loadingMessageId,
                    content = "",
                    isFromUser = false,
                    isLoading = true
                )
                addMessage(loadingMessage)

                _isLoading.value = true

                // 构建包含真实数据的提示
                val enrichedMessage = buildEnrichedPrompt(userMessage)

                Log.d(TAG, "Enriched prompt: $enrichedMessage")

                // 调用 Gemini API
                val response = chat.sendMessage(enrichedMessage)
                val aiResponse = response.text ?: """{"type":"text","message":"Sorry, I couldn't generate a response.","cardData":null}"""

                Log.d(TAG, "AI Response: $aiResponse")

                // 解析 AI 的 JSON 响应
                val parsedResponse = parseAIResponse(aiResponse)

                // 根据类型添加卡片
                when (parsedResponse.type) {
                    "shuttle" -> {
                        // 如果 AI 返回了卡片数据，使用它；否则使用本地生成的
                        val cardData = parsedResponse.cardData ?: createShuttleCardData()
                        val shuttleCard = NottiMessage(
                            id = generateMessageId(),
                            content = "",
                            isFromUser = false,
                            messageType = NottiMessageType.SHUTTLE_CARD,
                            cardData = cardData
                        )
                        // 先删除 loading 消息，添加卡片，再添加文字消息
                        removeMessage(loadingMessageId)
                        addMessage(shuttleCard)
                        addMessage(
                            NottiMessage(
                                id = generateMessageId(),
                                content = parsedResponse.message,
                                isFromUser = false,
                                isLoading = false
                            )
                        )
                    }
                    "booking" -> {
                        // 如果 AI 返回了卡片数据，使用它；否则使用本地生成的
                        val cardData = parsedResponse.cardData ?: createBookingCardData()
                        val bookingCard = NottiMessage(
                            id = generateMessageId(),
                            content = "",
                            isFromUser = false,
                            messageType = NottiMessageType.BOOKING_CARD,
                            cardData = cardData
                        )
                        // 先删除 loading 消息，添加卡片，再添加文字消息
                        removeMessage(loadingMessageId)
                        addMessage(bookingCard)
                        addMessage(
                            NottiMessage(
                                id = generateMessageId(),
                                content = parsedResponse.message,
                                isFromUser = false,
                                isLoading = false
                            )
                        )
                    }
                    else -> {
                        // 普通文字消息，更新 loading 消息
                        updateMessage(
                            loadingMessageId,
                            NottiMessage(
                                id = loadingMessageId,
                                content = parsedResponse.message,
                                isFromUser = false,
                                isLoading = false
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending message to Gemini", e)

                // 显示错误消息
                val errorMessage = when {
                    e.message?.contains("API key") == true -> "API configuration error. Please check Firebase setup."
                    e.message?.contains("network") == true -> "Network error. Please check your connection."
                    else -> "Sorry, something went wrong. Please try again."
                }

                // 使用具体的 loadingMessageId 更新错误状态
                updateMessage(
                    loadingMessageId,
                    NottiMessage(
                        id = loadingMessageId,
                        content = errorMessage,
                        isFromUser = false,
                        isError = true
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 解析 AI 的 JSON 响应
     */
    private fun parseAIResponse(response: String): NottiAIResponse {
        return try {
            // 尝试提取 JSON（AI 可能在 JSON 前后添加了其他文本）
            val jsonString = extractJson(response)
            val jsonObject = JSONObject(jsonString)

            val type = jsonObject.optString("type", "text")
            val message = jsonObject.optString("message", response)

            val cardData = if (jsonObject.has("cardData") && !jsonObject.isNull("cardData")) {
                parseCardData(jsonObject.getJSONObject("cardData"))
            } else null

            NottiAIResponse(type, message, cardData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response as JSON, treating as plain text", e)
            // 回退：将整个响应作为纯文本
            NottiAIResponse("text", response, null)
        }
    }

    /**
     * 从响应中提取 JSON 对象
     */
    private fun extractJson(response: String): String {
        // 尝试找到 JSON 对象的开始和结束
        val startIndex = response.indexOf('{')
        val endIndex = response.lastIndexOf('}')

        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            response.substring(startIndex, endIndex + 1)
        } else {
            response
        }
    }

    /**
     * 解析卡片数据
     */
    private fun parseCardData(json: JSONObject): NottiCardData {
        val title = json.optString("title", "")
        val subtitle = json.optString("subtitle", null)

        val items = mutableListOf<NottiCardItem>()
        if (json.has("items")) {
            val itemsArray = json.getJSONArray("items")
            for (i in 0 until itemsArray.length()) {
                val itemJson = itemsArray.getJSONObject(i)
                items.add(
                    NottiCardItem(
                        label = itemJson.optString("label", ""),
                        value = itemJson.optString("value", ""),
                        icon = itemJson.optString("icon", null),
                        isHighlighted = itemJson.optBoolean("isHighlighted", false)
                    )
                )
            }
        }

        return NottiCardData(title, subtitle, items)
    }

    /**
     * 移除消息
     */
    private fun removeMessage(messageId: String) {
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        currentMessages.removeAll { it.id == messageId }
        _messages.value = currentMessages
        // 不在 removeMessage 时保存，因为通常会紧跟其他消息操作
    }

    /**
     * 处理快捷操作
     */
    fun handleQuickAction(action: QuickAction) {
        val message = when (action) {
            QuickAction.SHUTTLE -> "What are the shuttle bus schedules today?"
            QuickAction.BOOKING -> "How can I book a sports facility?"
            QuickAction.EVENTS -> "Are there any campus events happening soon?"
            QuickAction.HELP -> "What can you help me with?"
        }
        sendMessage(message)
    }

    private fun addMessage(message: NottiMessage, shouldSave: Boolean = true) {
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        currentMessages.add(message)
        _messages.value = currentMessages
        // 只有非 loading 消息才保存到本地
        if (shouldSave && !message.isLoading) {
            saveChatHistory()
        }
    }

    private fun updateMessage(messageId: String, newMessage: NottiMessage) {
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        val index = currentMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            currentMessages[index] = newMessage
            _messages.value = currentMessages
            // 更新完成后保存（如果不是 loading 状态）
            if (!newMessage.isLoading) {
                saveChatHistory()
            }
        }
    }

    /**
     * 清空聊天记录
     */
    fun clearChat() {
        _messages.value = listOf(
            NottiMessage(
                content = "Hi! I'm Notti, your AI campus assistant. How can I help you today?",
                isFromUser = false
            )
        )
        // 清空后保存（只有欢迎消息）
        saveChatHistory()
    }

    enum class QuickAction {
        SHUTTLE, BOOKING, EVENTS, HELP
    }

    // ==================== 数据增强功能 ====================

    /**
     * 构建增强的提示，始终包含校车和预订的真实数据上下文
     * AI 将根据这些数据决定返回什么类型的响应
     */
    private suspend fun buildEnrichedPrompt(userMessage: String): String {
        // 始终获取校车和预订上下文，让 AI 决定是否使用
        val shuttleContext = getShuttleScheduleContext()
        val bookingContext = getBookingAvailabilityContext()

        return """
[AVAILABLE REAL-TIME DATA - Use this if relevant to user's question]

$shuttleContext

$bookingContext

[END OF DATA]

User's question: $userMessage

Remember: Respond in valid JSON format with type ("text", "shuttle", or "booking"), message, and cardData.
If showing shuttle or booking card, include cardData with the relevant information from the data above.
""".trim()
    }

    /**
     * 获取今天的校车时刻表上下文
     */
    private fun getShuttleScheduleContext(): String {
        val dayType = getCurrentDayType()
        val dayName = when (dayType) {
            DayType.WEEKDAY -> "Weekday (Monday-Thursday)"
            DayType.FRIDAY -> "Friday"
            DayType.WEEKEND -> "Weekend/Holiday"
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH)
        val currentDate = dateFormat.format(calendar.time)
        val currentTime = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(calendar.time)

        val scheduleBuilder = StringBuilder()
        scheduleBuilder.appendLine("=== SHUTTLE BUS SCHEDULE ===")
        scheduleBuilder.appendLine("Today: $currentDate ($dayName)")
        scheduleBuilder.appendLine("Current time: $currentTime")
        scheduleBuilder.appendLine()

        for (route in shuttleRoutes) {
            val schedule = when (dayType) {
                DayType.WEEKDAY -> route.weekdaySchedule
                DayType.FRIDAY -> route.fridaySchedule
                DayType.WEEKEND -> route.weekendSchedule
            }

            scheduleBuilder.appendLine("【${route.routeName}】${route.description}")

            if (schedule != null) {
                if (schedule.departureFromCampus.isNotEmpty()) {
                    scheduleBuilder.appendLine("  From Campus: ${schedule.departureFromCampus.joinToString(", ")}")
                }
                if (schedule.returnToCampus.isNotEmpty()) {
                    scheduleBuilder.appendLine("  To Campus: ${schedule.returnToCampus.joinToString(", ")}")
                }
            } else {
                scheduleBuilder.appendLine("  No service today")
            }

            route.specialNote?.let {
                scheduleBuilder.appendLine("  Note: $it")
            }
            scheduleBuilder.appendLine()
        }

        return scheduleBuilder.toString()
    }

    /**
     * 获取当前日期类型
     */
    private fun getCurrentDayType(): DayType {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY -> DayType.WEEKDAY
            Calendar.FRIDAY -> DayType.FRIDAY
            else -> DayType.WEEKEND
        }
    }

    /**
     * 获取预订可用性上下文
     */
    private suspend fun getBookingAvailabilityContext(): String {
        return try {
            val bookedSlots = getBookedSlotsFromFirebase()
            val facilities = listOf(
                "Basketball Court 1", "Basketball Court 2",
                "Badminton Court 1", "Badminton Court 2", "Badminton Court 3",
                "Tennis Court 1", "Tennis Court 2",
                "Squash Court 1", "Squash Court 2",
                "Football Pitch"
            )

            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val today = dateFormat.format(calendar.time)

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrow = dateFormat.format(calendar.time)

            val timeSlots = listOf(
                "8:00 AM - 9:00 AM", "9:00 AM - 10:00 AM", "10:00 AM - 11:00 AM",
                "11:00 AM - 12:00 PM", "12:00 PM - 1:00 PM", "1:00 PM - 2:00 PM",
                "2:00 PM - 3:00 PM", "3:00 PM - 4:00 PM", "4:00 PM - 5:00 PM",
                "5:00 PM - 6:00 PM", "6:00 PM - 7:00 PM", "7:00 PM - 8:00 PM",
                "8:00 PM - 9:00 PM", "9:00 PM - 10:00 PM"
            )

            val contextBuilder = StringBuilder()
            contextBuilder.appendLine("=== SPORTS FACILITY BOOKING ===")
            contextBuilder.appendLine("Available facilities: ${facilities.joinToString(", ")}")
            contextBuilder.appendLine()
            contextBuilder.appendLine("Booking fee: RM 5-15 per hour depending on facility")
            contextBuilder.appendLine()

            if (bookedSlots.isEmpty()) {
                contextBuilder.appendLine("✅ All time slots are currently available for booking!")
                contextBuilder.appendLine("Available time slots: ${timeSlots.joinToString(", ")}")
            } else {
                contextBuilder.appendLine("Currently booked slots:")
                bookedSlots.forEach { slot ->
                    contextBuilder.appendLine("  ❌ ${slot["facilityName"]} - ${formatBookingTime(slot)}")
                }
                contextBuilder.appendLine()
                contextBuilder.appendLine("Users can book available slots through the Booking section of the app.")
            }

            contextBuilder.appendLine()
            contextBuilder.appendLine("To book: Go to Booking > Select Facility > Choose Date & Time > Confirm")

            contextBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting booking context", e)
            """
=== SPORTS FACILITY BOOKING ===
Available facilities: Basketball Courts, Badminton Courts, Tennis Courts, Squash Courts, Football Pitch
Booking fee: RM 5-15 per hour depending on facility
To book: Go to Booking section in the app > Select a facility > Choose date and time > Confirm booking
"""
        }
    }

    /**
     * 从 Firebase 获取已预订的时段
     */
    private suspend fun getBookedSlotsFromFirebase(): List<Map<String, Any>> {
        return suspendCancellableCoroutine { continuation ->
            val calendar = Calendar.getInstance()
            val startOfToday = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            bookingsRef
                .orderByChild("startTime")
                .startAt(startOfToday.toDouble())
                .limitToFirst(20)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val bookings = mutableListOf<Map<String, Any>>()
                        for (child in snapshot.children) {
                            val booking = child.value as? Map<String, Any> ?: continue
                            val status = booking["status"] as? String ?: ""
                            // 只包含已确认或待处理的预订
                            if (status == "CONFIRMED" || status == "PENDING") {
                                bookings.add(booking)
                            }
                        }
                        continuation.resume(bookings)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase query cancelled: ${error.message}")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    /**
     * 格式化预订时间显示
     */
    private fun formatBookingTime(booking: Map<String, Any>): String {
        val startTime = booking["startTime"] as? Long ?: return "Unknown time"
        val endTime = booking["endTime"] as? Long ?: return "Unknown time"

        val dateFormat = SimpleDateFormat("MMM d", Locale.ENGLISH)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

        val date = dateFormat.format(startTime)
        val start = timeFormat.format(startTime)
        val end = timeFormat.format(endTime)

        return "$date, $start - $end"
    }

    /**
     * 加载校车时刻表数据
     */
    private fun loadShuttleRoutes(): List<ShuttleRoute> {
        return listOf(
            // Route #A: UNM TBS UNM
            ShuttleRoute(
                routeId = "A",
                routeName = "Route A",
                description = "UNM ↔ TBS",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:45pm"),
                    returnToCampus = listOf("7:45am")
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:45pm"),
                    returnToCampus = listOf("7:45am")
                ),
                weekendSchedule = null
            ),

            // Route #B: UNM Kj KTM/MRT UNM
            ShuttleRoute(
                routeId = "B",
                routeName = "Route B",
                description = "UNM ↔ Kajang KTM/MRT",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:00am", "11:15am", "1:15pm", "1:45pm", "3:15pm", "3:45pm",
                        "5:15pm", "5:45pm", "6:45pm", "7:15pm", "8:45pm", "10:30pm"
                    ),
                    returnToCampus = listOf(
                        "8:00am", "8:15am", "8:30am", "10:15am", "12:15pm", "2:30pm",
                        "4:30pm", "5:00pm", "6:30pm", "7:30pm", "8:00pm", "9:30pm"
                    )
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:00am", "11:15am", "1:15pm", "1:45pm", "2:45pm", "3:15pm",
                        "3:45pm", "4:15pm", "4:45pm", "5:15pm", "5:45pm", "6:45pm",
                        "7:15pm", "8:45pm", "10:30pm"
                    ),
                    returnToCampus = listOf(
                        "8:00am", "8:15am", "8:30am", "10:15am", "12:15pm", "2:30pm",
                        "4:30pm", "5:00pm", "6:30pm", "7:30pm", "8:00pm", "9:30pm"
                    )
                ),
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "7:30am", "9:30am", "11:30am", "12:30pm", "2:30pm", "3:30pm",
                        "4:30pm", "5:30pm", "6:30pm", "8:30pm", "10:30pm"
                    ),
                    returnToCampus = listOf(
                        "8:15am", "10:30am", "11:30am", "12:30pm", "2:30pm", "3:15pm",
                        "4:30pm", "5:15pm", "6:30pm", "7:30pm", "9:30pm", "11:30pm"
                    )
                ),
                specialNote = "Passes MRT Sg Jernih before Kajang KTM"
            ),

            // Route #C1: UNM TTS UNM
            ShuttleRoute(
                routeId = "C1",
                routeName = "Route C1",
                description = "UNM ↔ The Square (TTS)",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:30am", "10:30am", "11:30am", "12:00pm", "12:30pm",
                        "2:30pm", "3:00pm", "4:00pm", "5:00pm", "6:00pm",
                        "6:30pm", "7:00pm", "8:00pm", "9:30pm", "10:45pm", "12:00am"
                    ),
                    returnToCampus = listOf(
                        "9:40am", "10:40am", "11:40am", "12:10pm", "12:40pm",
                        "2:40pm", "3:10pm", "4:10pm", "5:10pm", "6:10pm",
                        "6:40pm", "7:10pm", "8:10pm", "9:40pm", "10:40pm"
                    )
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf(
                        "9:30am", "10:30am", "11:30am", "12:00pm", "2:30pm",
                        "3:00pm", "4:00pm", "5:00pm", "6:00pm", "6:30pm",
                        "7:00pm", "8:00pm", "9:30pm", "10:45pm", "12:00am"
                    ),
                    returnToCampus = listOf(
                        "9:40am", "10:40am", "11:40am", "12:10pm", "2:40pm",
                        "3:10pm", "4:10pm", "5:10pm", "6:10pm", "6:40pm",
                        "7:10pm", "8:10pm", "9:40pm", "10:40pm"
                    )
                ),
                weekendSchedule = null
            ),

            // Route #C2: TTS UNM
            ShuttleRoute(
                routeId = "C2",
                routeName = "Route C2",
                description = "TTS → UNM (Morning)",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = emptyList(),
                    returnToCampus = listOf("8:00am", "8:30am")
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = emptyList(),
                    returnToCampus = listOf("8:00am", "8:30am")
                ),
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf("12:30pm", "2:30pm", "6:45pm", "9:30pm", "11:00pm"),
                    returnToCampus = listOf("9:30am", "10:30am", "2:30pm", "6:15pm", "9:15pm", "11:00pm")
                ),
                specialNote = "Weekend service goes via TTS to IOI City Mall"
            ),

            // Route #D: UNM LOTUS Semenyih UNM
            ShuttleRoute(
                routeId = "D",
                routeName = "Route D",
                description = "UNM ↔ LOTUS Semenyih",
                weekdaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:30pm"),
                    returnToCampus = listOf("9:00pm")
                ),
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("6:30pm"),
                    returnToCampus = listOf("9:00pm")
                ),
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf("11:30am", "12:30pm"),
                    returnToCampus = listOf("3:15pm", "4:15pm")
                )
            ),

            // Route #E1: Friday Prayer Route
            ShuttleRoute(
                routeId = "E1",
                routeName = "Route E1",
                description = "UNM ↔ Al-Itt'a Mosque ↔ TTS",
                weekdaySchedule = null,
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("12:45pm", "1:00pm", "1:15pm"),
                    returnToCampus = listOf("2:00pm")
                ),
                weekendSchedule = null,
                specialNote = "Friday Prayer Service Only"
            ),

            // Route #E2: Friday Prayer Route
            ShuttleRoute(
                routeId = "E2",
                routeName = "Route E2",
                description = "UNM ↔ PGA Mosque",
                weekdaySchedule = null,
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("12:45pm", "1:00pm", "1:15pm"),
                    returnToCampus = listOf("2:00pm")
                ),
                weekendSchedule = null,
                specialNote = "Friday Prayer Service Only"
            ),

            // Route #G: UNM-IOI
            ShuttleRoute(
                routeId = "G",
                routeName = "Route G",
                description = "UNM ↔ IOI City Mall",
                weekdaySchedule = null,
                fridaySchedule = null,
                weekendSchedule = RouteSchedule(
                    departureFromCampus = listOf("12:30pm", "2:30pm", "6:45pm"),
                    returnToCampus = listOf("5:30pm", "8:30pm", "10:15pm")
                ),
                specialNote = "Weekend Only - via TTS. Not available on Public Holidays."
            )
        )
    }

    // ==================== 卡片生成功能 ====================

    /**
     * 创建校车时刻表卡片数据
     */
    private fun createShuttleCardData(): NottiCardData {
        val dayType = getCurrentDayType()
        val dayName = when (dayType) {
            DayType.WEEKDAY -> "Weekday"
            DayType.FRIDAY -> "Friday"
            DayType.WEEKEND -> "Weekend"
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM d", Locale.ENGLISH)
        val currentDate = dateFormat.format(calendar.time)

        // 构建路线项目列表
        val routeItems = mutableListOf<NottiCardItem>()

        for (route in shuttleRoutes) {
            val schedule = when (dayType) {
                DayType.WEEKDAY -> route.weekdaySchedule
                DayType.FRIDAY -> route.fridaySchedule
                DayType.WEEKEND -> route.weekendSchedule
            }

            if (schedule != null) {
                // 获取即将出发的时间（最多显示4个）
                val nextDepartures = if (schedule.departureFromCampus.isNotEmpty()) {
                    "→ ${schedule.departureFromCampus.take(4).joinToString(", ")}"
                } else if (schedule.returnToCampus.isNotEmpty()) {
                    "← ${schedule.returnToCampus.take(4).joinToString(", ")}"
                } else {
                    "No service"
                }

                routeItems.add(
                    NottiCardItem(
                        label = route.description,
                        value = nextDepartures,
                        icon = route.routeId
                    )
                )
            }
        }

        return NottiCardData(
            title = "Shuttle Schedule",
            subtitle = "$currentDate · $dayName",
            items = routeItems
        )
    }

    /**
     * 创建预订卡片数据
     */
    private suspend fun createBookingCardData(): NottiCardData {
        val bookedSlots = try {
            getBookedSlotsFromFirebase()
        } catch (e: Exception) {
            emptyList()
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM d", Locale.ENGLISH)
        val today = dateFormat.format(calendar.time)

        // 构建已预订时段列表
        val bookedItems = bookedSlots.map { slot ->
            NottiCardItem(
                label = slot["facilityName"]?.toString() ?: "Unknown",
                value = formatBookingTime(slot)
            )
        }

        return NottiCardData(
            title = "Sports Facility",
            subtitle = "Availability · $today",
            items = bookedItems
        )
    }
}
