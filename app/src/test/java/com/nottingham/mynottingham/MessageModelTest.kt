package com.nottingham.mynottingham

import com.nottingham.mynottingham.data.model.ChatMessage
import com.nottingham.mynottingham.data.model.Conversation
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Message data models
 *
 * Tests the messaging system data classes:
 * - Conversation data class
 * - ChatMessage data class
 */
class MessageModelTest {

    /**
     * Test Conversation creation with required fields
     */
    @Test
    fun `Conversation creation with required fields`() {
        val conversation = Conversation(
            id = "conv123",
            participantId = "user456",
            participantName = "John Doe",
            lastMessage = "Hello!",
            lastMessageTime = System.currentTimeMillis()
        )

        assertEquals("conv123", conversation.id)
        assertEquals("user456", conversation.participantId)
        assertEquals("John Doe", conversation.participantName)
        assertEquals("Hello!", conversation.lastMessage)
    }

    /**
     * Test Conversation default values
     */
    @Test
    fun `Conversation has correct default values`() {
        val conversation = Conversation(
            id = "conv123",
            participantId = "user456",
            participantName = "John",
            lastMessage = "Hi",
            lastMessageTime = 0L
        )

        assertNull("participantAvatar should be null by default", conversation.participantAvatar)
        assertEquals("unreadCount should be 0 by default", 0, conversation.unreadCount)
        assertFalse("isOnline should be false by default", conversation.isOnline)
        assertFalse("isPinned should be false by default", conversation.isPinned)
        assertFalse("isGroup should be false by default", conversation.isGroup)
    }

    /**
     * Test Conversation with all optional fields
     */
    @Test
    fun `Conversation with all optional fields`() {
        val conversation = Conversation(
            id = "conv123",
            participantId = "user456",
            participantName = "John Doe",
            participantAvatar = "avatar1",
            lastMessage = "See you tomorrow!",
            lastMessageTime = 1000L,
            unreadCount = 5,
            isOnline = true,
            isPinned = true,
            isGroup = false
        )

        assertEquals("avatar1", conversation.participantAvatar)
        assertEquals(5, conversation.unreadCount)
        assertTrue(conversation.isOnline)
        assertTrue(conversation.isPinned)
        assertFalse(conversation.isGroup)
    }

    /**
     * Test Conversation with group chat
     */
    @Test
    fun `Conversation as group chat`() {
        val groupConversation = Conversation(
            id = "group123",
            participantId = "group_participants",
            participantName = "Study Group",
            lastMessage = "Meeting at 3pm",
            lastMessageTime = 1000L,
            isGroup = true
        )

        assertTrue(groupConversation.isGroup)
        assertEquals("Study Group", groupConversation.participantName)
    }

    /**
     * Test Conversation with unread messages
     */
    @Test
    fun `Conversation with unread messages`() {
        val conversationWithUnread = Conversation(
            id = "conv123",
            participantId = "user456",
            participantName = "Jane",
            lastMessage = "Did you see my message?",
            lastMessageTime = 1000L,
            unreadCount = 3
        )

        val conversationRead = Conversation(
            id = "conv456",
            participantId = "user789",
            participantName = "Bob",
            lastMessage = "Thanks!",
            lastMessageTime = 2000L,
            unreadCount = 0
        )

        assertTrue(conversationWithUnread.unreadCount > 0)
        assertEquals(0, conversationRead.unreadCount)
    }

    /**
     * Test Conversation data class equality
     */
    @Test
    fun `Conversation data class equality`() {
        val conv1 = Conversation(
            id = "conv123",
            participantId = "user456",
            participantName = "John",
            lastMessage = "Hello",
            lastMessageTime = 1000L
        )

        val conv2 = Conversation(
            id = "conv123",
            participantId = "user456",
            participantName = "John",
            lastMessage = "Hello",
            lastMessageTime = 1000L
        )

        assertEquals("Conversations with same data should be equal", conv1, conv2)
    }

    /**
     * Test Conversation copy function
     */
    @Test
    fun `Conversation copy function works correctly`() {
        val original = Conversation(
            id = "conv123",
            participantId = "user456",
            participantName = "John",
            lastMessage = "Original message",
            lastMessageTime = 1000L,
            unreadCount = 5
        )

        val updated = original.copy(
            lastMessage = "New message",
            lastMessageTime = 2000L,
            unreadCount = 0
        )

        assertEquals("conv123", updated.id)
        assertEquals("New message", updated.lastMessage)
        assertEquals(2000L, updated.lastMessageTime)
        assertEquals(0, updated.unreadCount)
        assertEquals("Original message", original.lastMessage)
    }

    /**
     * Test ChatMessage creation with required fields
     */
    @Test
    fun `ChatMessage creation with required fields`() {
        val message = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John Doe",
            senderAvatar = null,
            message = "Hello, how are you?",
            timestamp = System.currentTimeMillis()
        )

        assertEquals("msg123", message.id)
        assertEquals("conv456", message.conversationId)
        assertEquals("user789", message.senderId)
        assertEquals("John Doe", message.senderName)
        assertEquals("Hello, how are you?", message.message)
    }

    /**
     * Test ChatMessage default values
     */
    @Test
    fun `ChatMessage has correct default values`() {
        val message = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "Hi",
            timestamp = 0L
        )

        assertFalse("isRead should be false by default", message.isRead)
        assertEquals("messageType should be TEXT by default", "TEXT", message.messageType)
        assertNull("imageUrl should be null by default", message.imageUrl)
    }

    /**
     * Test ChatMessage with avatar
     */
    @Test
    fun `ChatMessage with sender avatar`() {
        val message = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John Doe",
            senderAvatar = "avatar1",
            message = "Hello!",
            timestamp = 1000L
        )

        assertEquals("avatar1", message.senderAvatar)
    }

    /**
     * Test ChatMessage text type
     */
    @Test
    fun `ChatMessage text message type`() {
        val textMessage = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "This is a text message",
            timestamp = 1000L,
            messageType = "TEXT"
        )

        assertEquals("TEXT", textMessage.messageType)
        assertNull(textMessage.imageUrl)
    }

    /**
     * Test ChatMessage image type
     */
    @Test
    fun `ChatMessage image message type`() {
        val imageMessage = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "",
            timestamp = 1000L,
            messageType = "IMAGE",
            imageUrl = "https://example.com/image.jpg"
        )

        assertEquals("IMAGE", imageMessage.messageType)
        assertNotNull(imageMessage.imageUrl)
        assertEquals("https://example.com/image.jpg", imageMessage.imageUrl)
    }

    /**
     * Test ChatMessage file type
     */
    @Test
    fun `ChatMessage file message type`() {
        val fileMessage = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "document.pdf",
            timestamp = 1000L,
            messageType = "FILE"
        )

        assertEquals("FILE", fileMessage.messageType)
    }

    /**
     * Test ChatMessage read status
     */
    @Test
    fun `ChatMessage read status`() {
        val unreadMessage = ChatMessage(
            id = "msg1",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "New message",
            timestamp = 1000L,
            isRead = false
        )

        val readMessage = ChatMessage(
            id = "msg2",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "Old message",
            timestamp = 900L,
            isRead = true
        )

        assertFalse(unreadMessage.isRead)
        assertTrue(readMessage.isRead)
    }

    /**
     * Test ChatMessage data class equality
     */
    @Test
    fun `ChatMessage data class equality`() {
        val msg1 = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "Hello",
            timestamp = 1000L
        )

        val msg2 = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "Hello",
            timestamp = 1000L
        )

        assertEquals("Messages with same data should be equal", msg1, msg2)
    }

    /**
     * Test ChatMessage copy function
     */
    @Test
    fun `ChatMessage copy function works correctly`() {
        val original = ChatMessage(
            id = "msg123",
            conversationId = "conv456",
            senderId = "user789",
            senderName = "John",
            senderAvatar = null,
            message = "Original",
            timestamp = 1000L,
            isRead = false
        )

        val updated = original.copy(isRead = true)

        assertEquals("msg123", updated.id)
        assertEquals("Original", updated.message)
        assertTrue(updated.isRead)
        assertFalse(original.isRead)
    }

    /**
     * Test ChatMessage belongs to conversation
     */
    @Test
    fun `ChatMessage belongs to specific conversation`() {
        val msg1 = ChatMessage(
            id = "msg1",
            conversationId = "conv123",
            senderId = "user1",
            senderName = "User1",
            senderAvatar = null,
            message = "Message 1",
            timestamp = 1000L
        )

        val msg2 = ChatMessage(
            id = "msg2",
            conversationId = "conv123",
            senderId = "user2",
            senderName = "User2",
            senderAvatar = null,
            message = "Message 2",
            timestamp = 2000L
        )

        val msg3 = ChatMessage(
            id = "msg3",
            conversationId = "conv456",
            senderId = "user3",
            senderName = "User3",
            senderAvatar = null,
            message = "Message 3",
            timestamp = 3000L
        )

        assertEquals(msg1.conversationId, msg2.conversationId)
        assertNotEquals(msg1.conversationId, msg3.conversationId)
    }

    /**
     * Test Conversation pinned functionality
     */
    @Test
    fun `Conversation pinned functionality`() {
        val regularConv = Conversation(
            id = "conv1",
            participantId = "user1",
            participantName = "User",
            lastMessage = "Hello",
            lastMessageTime = 0L,
            isPinned = false
        )

        val pinnedConv = Conversation(
            id = "conv2",
            participantId = "user2",
            participantName = "Important Contact",
            lastMessage = "Important",
            lastMessageTime = 0L,
            isPinned = true
        )

        assertFalse(regularConv.isPinned)
        assertTrue(pinnedConv.isPinned)
    }

    /**
     * Test Conversation online status
     */
    @Test
    fun `Conversation online status`() {
        val offlineConv = Conversation(
            id = "conv1",
            participantId = "user1",
            participantName = "Offline User",
            lastMessage = "Message",
            lastMessageTime = 0L,
            isOnline = false
        )

        val onlineConv = Conversation(
            id = "conv2",
            participantId = "user2",
            participantName = "Online User",
            lastMessage = "Active now",
            lastMessageTime = 0L,
            isOnline = true
        )

        assertFalse(offlineConv.isOnline)
        assertTrue(onlineConv.isOnline)
    }

    /**
     * Test ChatMessage timestamp ordering
     */
    @Test
    fun `ChatMessage timestamp ordering`() {
        val messages = listOf(
            ChatMessage("m1", "conv1", "u1", "User1", null, "First", 1000L),
            ChatMessage("m2", "conv1", "u2", "User2", null, "Second", 2000L),
            ChatMessage("m3", "conv1", "u1", "User1", null, "Third", 3000L)
        )

        val sorted = messages.sortedBy { it.timestamp }

        assertEquals("m1", sorted[0].id)
        assertEquals("m2", sorted[1].id)
        assertEquals("m3", sorted[2].id)
    }
}
