package com.nottingham.mynottingham.ui.notti

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.nottingham.mynottingham.BuildConfig
import com.google.gson.annotations.SerializedName
import com.nottingham.mynottingham.data.local.NottiChatStorage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.nottingham.mynottingham.data.model.DayType
import com.nottingham.mynottingham.data.model.NottiCardData
import com.nottingham.mynottingham.data.model.NottiCardItem
import com.nottingham.mynottingham.data.model.NottiMessage
import com.nottingham.mynottingham.data.model.NottiMessageType
import com.nottingham.mynottingham.data.model.RouteSchedule
import com.nottingham.mynottingham.data.model.ShuttleRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume

/**
 * NottiViewModel - ViewModel for Notti AI Assistant
 *
 * Uses Google AI Studio (Gemini) to implement AI conversation functionality
 * Uses standard Google Generative AI SDK with API key
 * Chat history is cached locally per user account
 */
class NottiViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "NottiViewModel"

        // Claude API - Key should be set in local.properties or BuildConfig
        private const val CLAUDE_API_KEY = BuildConfig.CLAUDE_API_KEY
        private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"

        // System prompt - friendly assistant that can chat
        private const val BASE_SYSTEM_PROMPT = """You are Notti, a friendly AI assistant for University of Nottingham Malaysia (UNM) students.

OUTPUT JSON only:
{"intent":"shuttle|booking|general","message":"your response","cardData":null|{card}}

RULES:
- shuttle: bus/TBS/Kajang/TTS/IOI/LOTUS queries. ALL buses via UNM campus only!
- booking: sports/court/facility queries
- general: daily chat, weather, campus life, study tips, ANY other questions

For shuttle cardData: icon must be route ID (A/B/C1/C2/D/E1/E2/G), NOT emoji!
Example: {"icon":"C1","label":"UNM→TTS","value":"9:30am"}

You CAN answer general questions like weather, jokes, advice, campus life, etc.
Be friendly, helpful and conversational. Match user's language. Keep responses concise."""
    }

    // Chat message list
    private val _messages = MutableLiveData<List<NottiMessage>>(emptyList())
    val messages: LiveData<List<NottiMessage>> = _messages

    // Whether waiting for AI response
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Local chat history storage
    private val chatStorage = NottiChatStorage(application)
    private val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Firebase Database reference
    private val database = FirebaseDatabase.getInstance("https://mynottingham-b02b7-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val bookingsRef = database.getReference("bookings")

    // OkHttp client for Claude API
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    // Data classes for Claude API
    private data class ClaudeMessage(
        val role: String,  // "user" or "assistant"
        val content: String
    )

    private data class ClaudeRequest(
        val model: String,
        val max_tokens: Int,
        val system: String,
        val messages: List<ClaudeMessage>
    )

    private data class ClaudeResponse(
        val content: List<ClaudeContentBlock>?,
        val error: ClaudeError?
    )

    private data class ClaudeContentBlock(
        val type: String,
        val text: String?
    )

    private data class ClaudeError(
        val type: String?,
        val message: String?
    )

    // Shuttle bus schedule data
    private val shuttleRoutes: List<ShuttleRoute> by lazy { loadShuttleRoutes() }

    /**
     * Data class for AI response
     */
    private data class NottiAIResponse(
        val intent: String,  // "shuttle", "booking", "general"
        val message: String,
        val cardData: NottiCardData?  // AI-generated card data
    )

    init {
        // Load locally cached chat history
        loadChatHistory()
    }

    /**
     * Load chat history from local storage
     */
    private fun loadChatHistory() {
        val savedMessages = chatStorage.loadChatHistory(currentUserId)
        if (savedMessages.isNotEmpty()) {
            _messages.value = savedMessages
        } else {
            // No history, show welcome message
            addMessage(
                NottiMessage(
                    content = "Hi! I'm Notti, your AI campus assistant. How can I help you today?",
                    isFromUser = false
                )
            )
        }
    }

    /**
     * Save chat history to local storage
     */
    private fun saveChatHistory() {
        val currentMessages = _messages.value ?: return
        chatStorage.saveChatHistory(currentUserId, currentMessages)
    }

    // Message counter to ensure each message has unique ID
    private var messageCounter = 0L

    /**
     * Generate unique message ID
     */
    private fun generateMessageId(): String {
        return "${System.currentTimeMillis()}_${messageCounter++}"
    }

    /**
     * Send message to AI and get response
     */
    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Pre-generate loading message ID for use in catch block
        val loadingMessageId = generateMessageId()

        viewModelScope.launch {
            try {
                // Add user message - use unique ID
                val userNottiMessage = NottiMessage(
                    id = generateMessageId(),
                    content = userMessage.trim(),
                    isFromUser = true
                )
                addMessage(userNottiMessage)

                // Add loading AI message
                val loadingMessage = NottiMessage(
                    id = loadingMessageId,
                    content = "",
                    isFromUser = false,
                    isLoading = true
                )
                addMessage(loadingMessage)

                _isLoading.value = true

                // Build prompt with real data
                val enrichedMessage = buildEnrichedPrompt(userMessage)

                Log.d(TAG, "Enriched prompt: $enrichedMessage")

                // Call Claude API
                val aiResponse = callClaudeApi(enrichedMessage)

                Log.d(TAG, "AI Response: $aiResponse")

                // Parse AI's JSON response
                val parsedResponse = parseAIResponse(aiResponse)

                Log.d(TAG, "Parsed response: intent=${parsedResponse.intent}, hasCardData=${parsedResponse.cardData != null}")

                // Handle response based on intent type
                when {
                    parsedResponse.intent == "shuttle" && parsedResponse.cardData != null -> {
                        // Shuttle card - use AI-generated card data
                        val shuttleCard = NottiMessage(
                            id = generateMessageId(),
                            content = "",
                            isFromUser = false,
                            messageType = NottiMessageType.SHUTTLE_CARD,
                            cardData = parsedResponse.cardData
                        )
                        removeMessage(loadingMessageId)
                        // Add text message first, then card
                        addMessage(
                            NottiMessage(
                                id = generateMessageId(),
                                content = parsedResponse.message,
                                isFromUser = false,
                                isLoading = false
                            )
                        )
                        addMessage(shuttleCard)
                    }
                    parsedResponse.intent == "shuttle" && parsedResponse.cardData == null -> {
                        // Shuttle intent but AI didn't generate card data - use local data as fallback
                        val cardData = createShuttleCardData()
                        val shuttleCard = NottiMessage(
                            id = generateMessageId(),
                            content = "",
                            isFromUser = false,
                            messageType = NottiMessageType.SHUTTLE_CARD,
                            cardData = cardData
                        )
                        removeMessage(loadingMessageId)
                        // Add text message first, then card
                        addMessage(
                            NottiMessage(
                                id = generateMessageId(),
                                content = parsedResponse.message,
                                isFromUser = false,
                                isLoading = false
                            )
                        )
                        addMessage(shuttleCard)
                    }
                    parsedResponse.intent == "booking" -> {
                        // Booking card
                        val cardData = parsedResponse.cardData ?: createBookingCardData()
                        val bookingCard = NottiMessage(
                            id = generateMessageId(),
                            content = "",
                            isFromUser = false,
                            messageType = NottiMessageType.BOOKING_CARD,
                            cardData = cardData
                        )
                        removeMessage(loadingMessageId)
                        // Add text message first, then card
                        addMessage(
                            NottiMessage(
                                id = generateMessageId(),
                                content = parsedResponse.message,
                                isFromUser = false,
                                isLoading = false
                            )
                        )
                        addMessage(bookingCard)
                    }
                    else -> {
                        // Regular text message
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

                // Display error message
                val errorMessage = when {
                    e.message?.contains("API key") == true -> "API configuration error. Please check Firebase setup."
                    e.message?.contains("network") == true -> "Network error. Please check your connection."
                    else -> "Sorry, something went wrong. Please try again."
                }

                // Update error state using specific loadingMessageId
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
     * Call Claude API
     */
    private suspend fun callClaudeApi(userMessage: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Stateless mode - no conversation history to save tokens
                // Each request is independent, context is provided in the enriched prompt
                val messages = listOf(ClaudeMessage("user", userMessage))

                // Build request
                val request = ClaudeRequest(
                    model = "claude-sonnet-4-20250514",  // Sonnet 4.0
                    max_tokens = 800,  // Increased to avoid truncation
                    system = BASE_SYSTEM_PROMPT,
                    messages = messages
                )

                val jsonBody = gson.toJson(request)
                Log.d(TAG, "Claude request: $jsonBody")

                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url(CLAUDE_API_URL)
                    .addHeader("x-api-key", CLAUDE_API_KEY)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("anthropic-beta", "token-efficient-tools-2025-02-19")  // Token saving beta
                    .addHeader("content-type", "application/json")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(httpRequest).execute()
                val responseBody = response.body?.string() ?: ""

                Log.d(TAG, "Claude response code: ${response.code}")
                Log.d(TAG, "Claude response body: $responseBody")

                if (!response.isSuccessful) {
                    throw Exception("API error: ${response.code} - $responseBody")
                }

                val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)

                if (claudeResponse.error != null) {
                    throw Exception("Claude error: ${claudeResponse.error.message}")
                }

                claudeResponse.content?.firstOrNull { it.type == "text" }?.text
                    ?: """{"intent":"general","message":"Sorry, I couldn't generate a response.","cardData":null}"""
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Claude API", e)
                throw e
            }
        }
    }

    /**
     * Parse AI's JSON response
     * If JSON parsing fails, use keyword detection as fallback
     */
    private fun parseAIResponse(response: String): NottiAIResponse {
        return try {
            // Try to extract JSON (AI may have added other text before and after JSON)
            val jsonString = extractJson(response)
            val jsonObject = JSONObject(jsonString)

            // Support multiple formats
            val intent = jsonObject.optString("intent", null)
                ?: mapTypeToIntent(jsonObject.optString("type", "general"))
            val message = jsonObject.optString("message", response)

            // Parse card data
            val cardData = if (jsonObject.has("cardData") && !jsonObject.isNull("cardData")) {
                parseCardData(jsonObject.getJSONObject("cardData"))
            } else null

            Log.d(TAG, "JSON parsed: intent=$intent, hasCardData=${cardData != null}")
            NottiAIResponse(intent, message, cardData)
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse failed, using keyword detection: ${e.message}")
            // Fallback: use keyword detection for intent (cardData is null, use locally generated)
            detectIntentFromText(response)
        }
    }

    /**
     * Parse card data
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
     * Map old type format to new intent format
     */
    private fun mapTypeToIntent(type: String): String {
        return when (type.lowercase()) {
            "shuttle" -> "shuttle"
            "booking" -> "booking"
            else -> "general"
        }
    }

    /**
     * Detect intent from text (keyword matching fallback method)
     */
    private fun detectIntentFromText(text: String): NottiAIResponse {
        val lowerText = text.lowercase()

        // Shuttle bus keywords
        val shuttleKeywords = listOf(
            "shuttle", "bus", "shuttle", "shuttle", "transport",
            "tbs", "kajang", "ktm", "mrt", "tts", "the square",
            "ioi", "lotus", "semenyih", "mosque", "route",
            "schedule", "timetable", "departure", "schedule", "what time"
        )

        // Booking keywords
        val bookingKeywords = listOf(
            "book", "booking", "reserve", "booking", "book court",
            "basketball", "badminton", "tennis", "squash", "football",
            "basketball", "badminton", "tennis", "court", "facility", "sports",
            "sports", "court", "available"
        )

        val intent = when {
            shuttleKeywords.any { lowerText.contains(it) } -> "shuttle"
            bookingKeywords.any { lowerText.contains(it) } -> "booking"
            else -> "general"
        }

        // Clean response text (remove potential JSON remnants)
        val cleanMessage = text
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .replace(Regex("\\{[^}]*\\}"), "")
            .trim()
            .ifEmpty { text }

        Log.d(TAG, "Keyword detection: intent=$intent")
        // cardData is null, will use locally generated data
        return NottiAIResponse(intent, cleanMessage, null)
    }

    /**
     * Extract JSON object from response
     */
    private fun extractJson(response: String): String {
        // Remove potential markdown code block markers
        var cleaned = response
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        // Try to find the start and end of JSON object
        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')

        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            throw Exception("No valid JSON object found")
        }
    }

    /**
     * Remove message
     */
    private fun removeMessage(messageId: String) {
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        currentMessages.removeAll { it.id == messageId }
        _messages.value = currentMessages
        // Don't save when removing because it usually follows other message operations
    }

    /**
     * Handle quick actions - use local data directly without AI call
     */
    fun handleQuickAction(action: QuickAction) {
        viewModelScope.launch {
            when (action) {
                QuickAction.SHUTTLE -> {
                    // Direct local response - no AI needed
                    val cardData = createShuttleCardData()
                    addMessage(NottiMessage(
                        id = generateMessageId(),
                        content = "",
                        isFromUser = false,
                        messageType = NottiMessageType.SHUTTLE_CARD,
                        cardData = cardData
                    ))
                }
                QuickAction.BOOKING -> {
                    val cardData = createBookingCardData()
                    addMessage(NottiMessage(
                        id = generateMessageId(),
                        content = "",
                        isFromUser = false,
                        messageType = NottiMessageType.BOOKING_CARD,
                        cardData = cardData
                    ))
                }
                QuickAction.EVENTS -> {
                    addMessage(NottiMessage(
                        id = generateMessageId(),
                        content = "Campus events information coming soon! Check the university website for the latest updates.",
                        isFromUser = false
                    ))
                }
                QuickAction.HELP -> {
                    addMessage(NottiMessage(
                        id = generateMessageId(),
                        content = "I can help you with:\n• Shuttle bus schedules\n• Sports facility booking\n• Campus information\n\nJust ask me anything!",
                        isFromUser = false
                    ))
                }
            }
        }
    }

    private fun addMessage(message: NottiMessage, shouldSave: Boolean = true) {
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        currentMessages.add(message)
        _messages.value = currentMessages
        // Only non-loading messages are saved locally
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
            // Save after update (if not in loading state)
            if (!newMessage.isLoading) {
                saveChatHistory()
            }
        }
    }

    /**
     * Clear chat history
     */
    fun clearChat() {
        _messages.value = listOf(
            NottiMessage(
                content = "Hi! I'm Notti, your AI campus assistant. How can I help you today?",
                isFromUser = false
            )
        )
        // Save after clearing (only welcome message)
        saveChatHistory()
    }

    enum class QuickAction {
        SHUTTLE, BOOKING, EVENTS, HELP
    }

    // ==================== Data enrichment functionality ====================

    /**
     * Build enriched prompt
     * Provides complete context information including schedule data for accurate AI card generation
     */
    private suspend fun buildEnrichedPrompt(userMessage: String): String {
        // Get current time context
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH)
        val shortDateFormat = SimpleDateFormat("MMM d", Locale.ENGLISH)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        val currentDate = dateFormat.format(calendar.time)
        val currentShortDate = shortDateFormat.format(calendar.time)
        val currentTime = timeFormat.format(calendar.time)

        val todayDayType = getCurrentDayType()
        val todayDayTypeName = when (todayDayType) {
            DayType.WEEKDAY -> "Weekday"
            DayType.FRIDAY -> "Friday"
            DayType.WEEKEND -> "Weekend"
        }

        // Get tomorrow's date and type
        val tomorrowCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowDate = dateFormat.format(tomorrowCalendar.time)
        val tomorrowShortDate = shortDateFormat.format(tomorrowCalendar.time)
        val tomorrowDayType = when (tomorrowCalendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY -> DayType.WEEKDAY
            Calendar.FRIDAY -> DayType.FRIDAY
            else -> DayType.WEEKEND
        }
        val tomorrowDayTypeName = when (tomorrowDayType) {
            DayType.WEEKDAY -> "Weekday"
            DayType.FRIDAY -> "Friday"
            DayType.WEEKEND -> "Weekend"
        }

        // Get complete shuttle schedule data
        val shuttleData = getFullShuttleScheduleData()

        return """
[DATE CONTEXT]
TODAY: $currentDate
- Short format: $currentShortDate
- Day type: $todayDayTypeName
- Current time: $currentTime

TOMORROW: $tomorrowDate
- Short format: $tomorrowShortDate
- Day type: $tomorrowDayTypeName

[SHUTTLE DATA]
$shuttleData

[USER MESSAGE]
$userMessage

[INSTRUCTIONS]
1. Determine TARGET DATE from user's message (today → TODAY, tomorrow → TOMORROW)
2. Use the TARGET DATE's day type to filter routes (only include routes with service on that day)
3. Generate cardData with subtitle format: "$tomorrowShortDate · $tomorrowDayTypeName (Tomorrow)" for tomorrow
4. ONLY output valid JSON with intent, message, and cardData
""".trim()
    }

    /**
     * Get complete shuttle schedule data (for AI use)
     */
    private fun getFullShuttleScheduleData(): String {
        val scheduleBuilder = StringBuilder()

        for (route in shuttleRoutes) {
            scheduleBuilder.appendLine("[${route.routeId}] ${route.routeName}: ${route.description}")

            // Weekday schedule
            if (route.weekdaySchedule != null) {
                scheduleBuilder.append("  WEEKDAY: ")
                if (route.weekdaySchedule.departureFromCampus.isNotEmpty()) {
                    scheduleBuilder.append("From Campus: ${route.weekdaySchedule.departureFromCampus.joinToString(", ")}")
                }
                if (route.weekdaySchedule.returnToCampus.isNotEmpty()) {
                    if (route.weekdaySchedule.departureFromCampus.isNotEmpty()) scheduleBuilder.append(" | ")
                    scheduleBuilder.append("To Campus: ${route.weekdaySchedule.returnToCampus.joinToString(", ")}")
                }
                scheduleBuilder.appendLine()
            } else {
                scheduleBuilder.appendLine("  WEEKDAY: No service")
            }

            // Friday schedule
            if (route.fridaySchedule != null) {
                scheduleBuilder.append("  FRIDAY: ")
                if (route.fridaySchedule.departureFromCampus.isNotEmpty()) {
                    scheduleBuilder.append("From Campus: ${route.fridaySchedule.departureFromCampus.joinToString(", ")}")
                }
                if (route.fridaySchedule.returnToCampus.isNotEmpty()) {
                    if (route.fridaySchedule.departureFromCampus.isNotEmpty()) scheduleBuilder.append(" | ")
                    scheduleBuilder.append("To Campus: ${route.fridaySchedule.returnToCampus.joinToString(", ")}")
                }
                scheduleBuilder.appendLine()
            } else {
                scheduleBuilder.appendLine("  FRIDAY: No service")
            }

            // Weekend schedule
            if (route.weekendSchedule != null) {
                scheduleBuilder.append("  WEEKEND: ")
                if (route.weekendSchedule.departureFromCampus.isNotEmpty()) {
                    scheduleBuilder.append("From Campus: ${route.weekendSchedule.departureFromCampus.joinToString(", ")}")
                }
                if (route.weekendSchedule.returnToCampus.isNotEmpty()) {
                    if (route.weekendSchedule.departureFromCampus.isNotEmpty()) scheduleBuilder.append(" | ")
                    scheduleBuilder.append("To Campus: ${route.weekendSchedule.returnToCampus.joinToString(", ")}")
                }
                scheduleBuilder.appendLine()
            } else {
                scheduleBuilder.appendLine("  WEEKEND: No service")
            }

            route.specialNote?.let {
                scheduleBuilder.appendLine("  Note: $it")
            }
            scheduleBuilder.appendLine()
        }

        return scheduleBuilder.toString()
    }

    /**
     * Get today's shuttle schedule context
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

            scheduleBuilder.appendLine("[${route.routeName}] ${route.description}")

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
     * Get current day type
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
     * Get booking availability context
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
                contextBuilder.appendLine("All time slots are currently available for booking!")
                contextBuilder.appendLine("Available time slots: ${timeSlots.joinToString(", ")}")
            } else {
                contextBuilder.appendLine("Currently booked slots:")
                bookedSlots.forEach { slot ->
                    contextBuilder.appendLine("  ${slot["facilityName"]} - ${formatBookingTime(slot)}")
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
     * Get booked time slots from Firebase
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
                            // Only include confirmed or pending bookings
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
     * Format booking time display
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
     * Load shuttle schedule data
     */
    private fun loadShuttleRoutes(): List<ShuttleRoute> {
        return listOf(
            // Route A: UNM TBS UNM
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

            // Route B: UNM Kajang KTM/MRT UNM
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

            // Route C1: UNM TTS UNM
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

            // Route C2: TTS UNM
            ShuttleRoute(
                routeId = "C2",
                routeName = "Route C2",
                description = "TTS to UNM (Morning)",
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

            // Route D: UNM LOTUS Semenyih UNM
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

            // Route E1: Friday Prayer Route
            ShuttleRoute(
                routeId = "E1",
                routeName = "Route E1",
                description = "UNM ↔ Al-Itta Mosque ↔ TTS",
                weekdaySchedule = null,
                fridaySchedule = RouteSchedule(
                    departureFromCampus = listOf("12:45pm", "1:00pm", "1:15pm"),
                    returnToCampus = listOf("2:00pm")
                ),
                weekendSchedule = null,
                specialNote = "Friday Prayer Service Only"
            ),

            // Route E2: Friday Prayer Route
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

            // Route G: UNM-IOI
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

    // ==================== Card generation functionality ====================

    /**
     * Create shuttle schedule card data
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

        // Build route items list
        val routeItems = mutableListOf<NottiCardItem>()

        for (route in shuttleRoutes) {
            val schedule = when (dayType) {
                DayType.WEEKDAY -> route.weekdaySchedule
                DayType.FRIDAY -> route.fridaySchedule
                DayType.WEEKEND -> route.weekendSchedule
            }

            if (schedule != null) {
                // Get upcoming departure times (show max 4)
                val nextDepartures = if (schedule.departureFromCampus.isNotEmpty()) {
                    "to ${schedule.departureFromCampus.take(4).joinToString(", ")}"
                } else if (schedule.returnToCampus.isNotEmpty()) {
                    "from ${schedule.returnToCampus.take(4).joinToString(", ")}"
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
     * Create booking card data
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

        // Build booked time slots list
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
