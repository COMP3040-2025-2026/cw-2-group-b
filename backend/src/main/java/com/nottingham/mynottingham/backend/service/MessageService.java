package com.nottingham.mynottingham.backend.service;

import com.nottingham.mynottingham.backend.dto.ConversationDto;
import com.nottingham.mynottingham.backend.dto.MessageDto;
import com.nottingham.mynottingham.backend.dto.ParticipantDto;
import com.nottingham.mynottingham.backend.dto.SendMessageRequest;
import com.nottingham.mynottingham.backend.entity.Conversation;
import com.nottingham.mynottingham.backend.entity.ConversationParticipant;
import com.nottingham.mynottingham.backend.entity.Message;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.repository.ConversationParticipantRepository;
import com.nottingham.mynottingham.backend.repository.ConversationRepository;
import com.nottingham.mynottingham.backend.repository.MessageRepository;
import com.nottingham.mynottingham.backend.websocket.MessageWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessageService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageWebSocketHandler webSocketHandler;

    /**
     * Create a new conversation
     */
    public ConversationDto createConversation(Long currentUserId, List<Long> participantIds, Boolean isGroup, String groupName) {
        // Validate participants
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("Participant list cannot be empty");
        }

        // Include current user in participants if not already present
        if (!participantIds.contains(currentUserId)) {
            participantIds = new ArrayList<>(participantIds);
            participantIds.add(0, currentUserId);
        }

        // For one-on-one chats, check if conversation already exists
        if (!isGroup && participantIds.size() == 2) {
            Long user1Id = participantIds.get(0);
            Long user2Id = participantIds.get(1);
            Optional<Conversation> existing = conversationRepository.findOneOnOneConversation(user1Id, user2Id);
            if (existing.isPresent()) {
                return convertToDto(existing.get(), currentUserId);
            }
        }

        // Create new conversation
        Conversation conversation = new Conversation();
        conversation.setConversationUuid(UUID.randomUUID().toString());
        conversation.setIsGroup(isGroup != null && isGroup);
        conversation.setGroupName(groupName);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        // Save conversation first
        conversation = conversationRepository.save(conversation);

        // Add participants
        List<ConversationParticipant> participants = new ArrayList<>();
        for (Long userId : participantIds) {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            ConversationParticipant participant = new ConversationParticipant();
            participant.setConversation(conversation);
            participant.setUser(user);
            participant.setJoinedAt(LocalDateTime.now());
            participant.setIsPinned(false);
            participant.setUnreadCount(0);

            participants.add(participantRepository.save(participant));
        }

        conversation.setParticipants(participants);

        return convertToDto(conversation, currentUserId);
    }

    /**
     * Get all conversations for a user
     */
    public List<ConversationDto> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        return conversations.stream()
                .map(c -> convertToDto(c, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get conversation by UUID
     */
    public Optional<ConversationDto> getConversationByUuid(String uuid, Long currentUserId) {
        return conversationRepository.findByConversationUuid(uuid)
                .map(c -> convertToDto(c, currentUserId));
    }

    /**
     * Delete conversation
     */
    public void deleteConversation(String conversationUuid, Long currentUserId) {
        // Find conversation by UUID
        Conversation conversation = conversationRepository.findByConversationUuid(conversationUuid)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Verify user is a participant
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUserId));

        if (!isParticipant) {
            throw new IllegalArgumentException("User is not a participant of this conversation");
        }

        // Delete all participants first (cascade will handle this, but being explicit)
        participantRepository.deleteByConversationId(conversation.getId());

        // Delete the conversation
        conversationRepository.delete(conversation);
    }

    /**
     * Convert Conversation entity to DTO
     */
    private ConversationDto convertToDto(Conversation conversation, Long currentUserId) {
        List<ParticipantDto> participantDtos = conversation.getParticipants().stream()
                .map(this::convertParticipantToDto)
                .collect(Collectors.toList());

        return ConversationDto.builder()
                .id(conversation.getConversationUuid())
                .isGroup(conversation.getIsGroup())
                .groupName(conversation.getGroupName())
                .groupAvatar(conversation.getGroupAvatar())
                .lastMessage(conversation.getLastMessage())
                .lastMessageTime(toTimestamp(conversation.getLastMessageTime()))
                .lastMessageSenderId(conversation.getLastMessageSenderId() != null ?
                        conversation.getLastMessageSenderId().toString() : null)
                .unreadCount(getUnreadCount(conversation, currentUserId))
                .isPinned(isPinned(conversation, currentUserId))
                .participants(participantDtos)
                .createdAt(toTimestamp(conversation.getCreatedAt()))
                .updatedAt(toTimestamp(conversation.getUpdatedAt()))
                .build();
    }

    /**
     * Convert ConversationParticipant to ParticipantDto
     */
    private ParticipantDto convertParticipantToDto(ConversationParticipant participant) {
        User user = participant.getUser();
        return ParticipantDto.builder()
                .userId(user.getId().toString())
                .userName(user.getFullName())
                .userAvatar(user.getAvatarUrl())
                .isOnline(false) // TODO: implement online status
                .isTyping(false)
                .lastSeenAt(toTimestamp(LocalDateTime.now()))
                .build();
    }

    /**
     * Get unread count for current user in a conversation
     */
    private Integer getUnreadCount(Conversation conversation, Long currentUserId) {
        return participantRepository.findByConversationIdAndUserId(conversation.getId(), currentUserId)
                .map(ConversationParticipant::getUnreadCount)
                .orElse(0);
    }

    /**
     * Check if conversation is pinned for current user
     */
    private Boolean isPinned(Conversation conversation, Long currentUserId) {
        return participantRepository.findByConversationIdAndUserId(conversation.getId(), currentUserId)
                .map(ConversationParticipant::getIsPinned)
                .orElse(false);
    }

    /**
     * Convert LocalDateTime to timestamp (milliseconds)
     */
    private Long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return System.currentTimeMillis();
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Send a message in a conversation
     */
    public MessageDto sendMessage(String conversationUuid, Long senderId, SendMessageRequest request) {
        // Find conversation by UUID
        Conversation conversation = conversationRepository.findByConversationUuid(conversationUuid)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Verify sender is a participant
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(senderId));

        if (!isParticipant) {
            throw new IllegalArgumentException("User is not a participant of this conversation");
        }

        // Get sender user
        User sender = userService.getUserById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // Create message
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        // Set message type
        if (request.getMessageType() != null) {
            message.setType(Message.MessageType.valueOf(request.getMessageType()));
        } else {
            message.setType(Message.MessageType.TEXT);
        }

        // Set attachment URL if provided
        if (request.getAttachmentUrl() != null && !request.getAttachmentUrl().isEmpty()) {
            message.setAttachmentUrl(request.getAttachmentUrl());
        }

        // Set initial status
        message.setStatus(Message.MessageStatus.SENT);

        // Save message
        message = messageRepository.save(message);

        // Update conversation's last message info
        conversation.setLastMessage(request.getContent());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversation.setLastMessageSenderId(senderId);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Increment unread count for all participants except sender
        for (ConversationParticipant participant : conversation.getParticipants()) {
            if (!participant.getUser().getId().equals(senderId)) {
                participant.setUnreadCount(participant.getUnreadCount() + 1);
                participantRepository.save(participant);
            }
        }

        // Notify via WebSocket
        MessageDto messageDto = convertMessageToDto(message);
        Map<String, Object> wsData = new HashMap<>();
        wsData.put("conversationId", conversationUuid);
        wsData.put("message", messageDto);

        MessageWebSocketHandler.WebSocketMessage wsMessage =
            new MessageWebSocketHandler.WebSocketMessage("NEW_MESSAGE", "New message received", wsData);
        webSocketHandler.sendToConversation(conversationUuid, wsMessage);

        return messageDto;
    }

    /**
     * Get messages in a conversation with pagination
     */
    public Page<MessageDto> getMessages(String conversationUuid, Long userId, int page, int size) {
        // Find conversation by UUID
        Conversation conversation = conversationRepository.findByConversationUuid(conversationUuid)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Verify user is a participant
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new IllegalArgumentException("User is not a participant of this conversation");
        }

        // Create pageable (descending order - most recent first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Get messages with pagination
        Page<Message> messagePage = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);

        // Convert to DTOs
        return messagePage.map(this::convertMessageToDto);
    }

    /**
     * Mark all messages in a conversation as read for the current user
     */
    public void markConversationAsRead(String conversationUuid, Long userId) {
        // Find conversation by UUID
        Conversation conversation = conversationRepository.findByConversationUuid(conversationUuid)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Mark all messages as read (messages not sent by this user)
        messageRepository.markAllAsRead(conversation.getId(), userId);

        // Reset unread count for this user's participant entry
        Optional<ConversationParticipant> participantOpt =
                participantRepository.findByConversationIdAndUserId(conversation.getId(), userId);

        participantOpt.ifPresent(participant -> {
            participant.setUnreadCount(0);
            participantRepository.save(participant);
        });
    }

    /**
     * Update pinned status for a conversation
     */
    public void updatePinnedStatus(String conversationUuid, Long userId, Boolean isPinned) {
        // Find conversation by UUID
        Conversation conversation = conversationRepository.findByConversationUuid(conversationUuid)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Find participant entry for this user
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversation.getId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant"));

        // Update pinned status
        participant.setIsPinned(isPinned);
        participantRepository.save(participant);
    }

    /**
     * Delete a single message
     */
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        // Verify user is the sender
        if (!message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the sender can delete this message");
        }

        messageRepository.delete(message);
    }

    /**
     * Convert Message entity to MessageDto
     */
    private MessageDto convertMessageToDto(Message message) {
        MessageDto dto = new MessageDto();

        // Convert Long to String for Android compatibility
        dto.setId(String.valueOf(message.getId()));
        dto.setConversationId(message.getConversation().getConversationUuid());
        dto.setSenderId(String.valueOf(message.getSender().getId()));
        dto.setSenderName(message.getSender().getFullName());
        dto.setSenderAvatar(message.getSender().getAvatarUrl());
        dto.setContent(message.getContent());

        // Convert LocalDateTime to Unix timestamp (milliseconds)
        long timestamp = message.getCreatedAt()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        dto.setTimestamp(timestamp);
        dto.setCreatedAt(timestamp);

        dto.setIsRead(message.getIsRead());
        dto.setMessageType(message.getType().name());
        dto.setAttachmentUrl(message.getAttachmentUrl());
        dto.setStatus(message.getStatus().name());
        dto.setUpdatedAt(message.getUpdatedAt());
        return dto;
    }
}
