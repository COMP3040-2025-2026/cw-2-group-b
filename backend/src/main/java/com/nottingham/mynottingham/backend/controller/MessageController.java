package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.*;
import com.nottingham.mynottingham.backend.dto.response.ApiResponse;
import com.nottingham.mynottingham.backend.entity.Student;
import com.nottingham.mynottingham.backend.entity.Teacher;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/message")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private UserService userService;

    /**
     * Get contact suggestions for current user
     * Students see their teachers, teachers see their students
     */
    @GetMapping("/contacts/suggestions")
    public ResponseEntity<ApiResponse<List<ContactSuggestionDto>>> getContactSuggestions(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // Extract user ID from token (simplified - in production use JWT)
            // For now, return all users except current user
            List<User> allUsers = userService.getAllUsers();

            List<ContactSuggestionDto> suggestions = new ArrayList<>();

            for (User user : allUsers) {
                ContactSuggestionDto dto = ContactSuggestionDto.builder()
                        .userId(user.getId().toString())
                        .userName(user.getFullName())
                        .userAvatar(user.getAvatarUrl())
                        .isOnline(false) // Default to offline for now
                        .build();

                // Add student-specific info
                if (user instanceof Student) {
                    Student student = (Student) user;
                    dto.setProgram(student.getMajor());
                    dto.setYear(student.getYearOfStudy());
                }

                suggestions.add(dto);
            }

            return ResponseEntity.ok(ApiResponse.success(suggestions));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to get contact suggestions: " + e.getMessage()));
        }
    }

    /**
     * Get default contacts for a user
     * Students get their enrolled course teachers
     * Teachers get their students
     */
    @GetMapping("/contacts/default/{userId}")
    public ResponseEntity<ApiResponse<List<ContactSuggestionDto>>> getDefaultContacts(
            @PathVariable Long userId) {
        try {
            List<ContactSuggestionDto> defaultContacts = new ArrayList<>();

            User user = userService.getUserById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error("User not found"));
            }

            if (user instanceof Student) {
                // Get student's teachers
                Student student = (Student) user;
                // Get enrollments to find teachers
                student.getEnrollments().forEach(enrollment -> {
                    Teacher teacher = enrollment.getCourse().getTeacher();
                    if (teacher != null) {
                        ContactSuggestionDto dto = ContactSuggestionDto.builder()
                                .userId(teacher.getId().toString())
                                .userName(teacher.getFullName())
                                .userAvatar(teacher.getAvatarUrl())
                                .program(teacher.getDepartment())
                                .isOnline(false)
                                .build();
                        // Avoid duplicates
                        if (defaultContacts.stream().noneMatch(c -> c.getUserId().equals(dto.getUserId()))) {
                            defaultContacts.add(dto);
                        }
                    }
                });
            } else if (user instanceof Teacher) {
                // Get teacher's students
                Teacher teacher = (Teacher) user;
                teacher.getCourses().forEach(course -> {
                    course.getEnrollments().forEach(enrollment -> {
                        Student student = enrollment.getStudent();
                        if (student != null) {
                            ContactSuggestionDto dto = ContactSuggestionDto.builder()
                                    .userId(student.getId().toString())
                                    .userName(student.getFullName())
                                    .userAvatar(student.getAvatarUrl())
                                    .program(student.getMajor())
                                    .year(student.getYearOfStudy())
                                    .isOnline(false)
                                    .build();
                            // Avoid duplicates
                            if (defaultContacts.stream().noneMatch(c -> c.getUserId().equals(dto.getUserId()))) {
                                defaultContacts.add(dto);
                            }
                        }
                    });
                });
            }

            return ResponseEntity.ok(ApiResponse.success(defaultContacts));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to get default contacts: " + e.getMessage()));
        }
    }

    /**
     * Create a new conversation
     */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody CreateConversationRequest request) {
        try {
            // Generate conversation ID
            String conversationId = UUID.randomUUID().toString();
            long currentTime = System.currentTimeMillis();

            // Get participant details
            List<ParticipantDto> participants = new ArrayList<>();
            for (String participantId : request.getParticipantIds()) {
                try {
                    Long userId = Long.parseLong(participantId);
                    userService.getUserById(userId).ifPresent(user -> {
                        ParticipantDto participant = ParticipantDto.builder()
                                .userId(user.getId().toString())
                                .userName(user.getFullName())
                                .userAvatar(user.getAvatarUrl())
                                .isOnline(false)
                                .isTyping(false)
                                .lastSeenAt(currentTime)
                                .build();
                        participants.add(participant);
                    });
                } catch (NumberFormatException e) {
                    // Skip invalid user ID
                }
            }

            // Create conversation DTO
            ConversationDto conversation = ConversationDto.builder()
                    .id(conversationId)
                    .isGroup(request.getIsGroup() != null && request.getIsGroup())
                    .groupName(request.getGroupName())
                    .groupAvatar(null)
                    .lastMessage(null)
                    .lastMessageTime(currentTime)
                    .lastMessageSenderId(null)
                    .unreadCount(0)
                    .isPinned(false)
                    .participants(participants)
                    .createdAt(currentTime)
                    .updatedAt(currentTime)
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Conversation created successfully", conversation));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to create conversation: " + e.getMessage()));
        }
    }

    /**
     * Get all conversations for current user
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getConversations(
            @RequestHeader(value = "Authorization", required = false) String token) {
        // Return empty list for now - frontend will create conversations locally
        return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
    }

    /**
     * Get conversation by ID
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String conversationId) {
        return ResponseEntity.ok(ApiResponse.error("Conversation not found"));
    }

    /**
     * Update pinned status
     */
    @PutMapping("/conversations/{conversationId}/pin")
    public ResponseEntity<ApiResponse<ConversationDto>> updatePinnedStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String conversationId,
            @RequestBody UpdatePinnedStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pinned status updated", null));
    }

    /**
     * Mark conversation as read
     */
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Object>> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String conversationId) {
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }
}
