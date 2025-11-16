package com.nottingham.mynottingham.backend.service;

import com.nottingham.mynottingham.backend.dto.ConversationDto;
import com.nottingham.mynottingham.backend.dto.ParticipantDto;
import com.nottingham.mynottingham.backend.entity.Conversation;
import com.nottingham.mynottingham.backend.entity.ConversationParticipant;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.repository.ConversationParticipantRepository;
import com.nottingham.mynottingham.backend.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
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
    private UserService userService;

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
}
