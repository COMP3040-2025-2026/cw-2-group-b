package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.*;
import com.nottingham.mynottingham.backend.dto.response.ApiResponse;
import com.nottingham.mynottingham.backend.entity.Student;
import com.nottingham.mynottingham.backend.entity.Teacher;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.repository.StudentRepository;
import com.nottingham.mynottingham.backend.repository.TeacherRepository;
import com.nottingham.mynottingham.backend.service.MessageService;
import com.nottingham.mynottingham.backend.service.UserService;
import com.nottingham.mynottingham.backend.util.JwtUtil;
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

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MessageService messageService;

    /**
     * Get contact suggestions for current user
     * Students see their faculty first, then other faculties (relaxed filtering)
     * Teachers see their department first, then other departments
     */
    @GetMapping("/contacts/suggestions")
    public ResponseEntity<ApiResponse<List<ContactSuggestionDto>>> getContactSuggestions(
            @RequestHeader(value = "Authorization") String token) {
        try {
            // Extract user ID from JWT token
            Long currentUserId = jwtUtil.extractUserId(token);
            User currentUser = userService.getUserById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            List<ContactSuggestionDto> sameFacultySuggestions = new ArrayList<>();
            List<ContactSuggestionDto> otherFacultySuggestions = new ArrayList<>();

            if (currentUser instanceof Student) {
                Student student = (Student) currentUser;
                String currentFaculty = student.getFaculty();

                // Get same faculty students and teachers
                List<Student> sameFacultyStudents = studentRepository.findByFaculty(currentFaculty);
                List<Teacher> sameFacultyTeachers = teacherRepository.findByDepartment(currentFaculty);

                // Get other faculty students and teachers
                List<Student> allStudents = studentRepository.findAll();
                List<Teacher> allTeachers = teacherRepository.findAll();

                // Build same faculty contacts
                for (Student s : sameFacultyStudents) {
                    if (!s.getId().equals(currentUserId)) {
                        sameFacultySuggestions.add(buildContactDto(s, true));
                    }
                }
                for (Teacher t : sameFacultyTeachers) {
                    sameFacultySuggestions.add(buildContactDto(t, true));
                }

                // Build other faculty contacts
                for (Student s : allStudents) {
                    if (!s.getId().equals(currentUserId) && !currentFaculty.equals(s.getFaculty())) {
                        otherFacultySuggestions.add(buildContactDto(s, false));
                    }
                }
                for (Teacher t : allTeachers) {
                    if (!currentFaculty.equals(t.getDepartment())) {
                        otherFacultySuggestions.add(buildContactDto(t, false));
                    }
                }

            } else if (currentUser instanceof Teacher) {
                Teacher teacher = (Teacher) currentUser;
                String currentDepartment = teacher.getDepartment();

                // Get same department teachers and students
                List<Teacher> sameDeptTeachers = teacherRepository.findByDepartment(currentDepartment);
                List<Student> sameDeptStudents = studentRepository.findByFaculty(currentDepartment);

                // Get all teachers and students
                List<Teacher> allTeachers = teacherRepository.findAll();
                List<Student> allStudents = studentRepository.findAll();

                // Build same department contacts
                for (Teacher t : sameDeptTeachers) {
                    if (!t.getId().equals(currentUserId)) {
                        sameFacultySuggestions.add(buildContactDto(t, true));
                    }
                }
                for (Student s : sameDeptStudents) {
                    sameFacultySuggestions.add(buildContactDto(s, true));
                }

                // Build other department contacts
                for (Teacher t : allTeachers) {
                    if (!t.getId().equals(currentUserId) && !currentDepartment.equals(t.getDepartment())) {
                        otherFacultySuggestions.add(buildContactDto(t, false));
                    }
                }
                for (Student s : allStudents) {
                    if (!currentDepartment.equals(s.getFaculty())) {
                        otherFacultySuggestions.add(buildContactDto(s, false));
                    }
                }
            }

            // Combine lists: same faculty first, then others
            List<ContactSuggestionDto> allSuggestions = new ArrayList<>();
            allSuggestions.addAll(sameFacultySuggestions);
            allSuggestions.addAll(otherFacultySuggestions);

            return ResponseEntity.ok(ApiResponse.success(allSuggestions));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to get contact suggestions: " + e.getMessage()));
        }
    }

    /**
     * Helper method to build ContactSuggestionDto
     */
    private ContactSuggestionDto buildContactDto(User user, boolean isSameFaculty) {
        ContactSuggestionDto.ContactSuggestionDtoBuilder builder = ContactSuggestionDto.builder()
                .userId(user.getId().toString())
                .userName(user.getFullName())
                .userAvatar(user.getAvatarUrl())
                .isOnline(false);

        // Add student-specific info
        if (user instanceof Student) {
            Student student = (Student) user;
            builder.program(student.getMajor())
                   .year(student.getYearOfStudy());
        }

        // Add teacher-specific info
        if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            builder.program(teacher.getDepartment());
        }

        return builder.build();
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
            @RequestHeader(value = "Authorization") String token,
            @RequestBody CreateConversationRequest request) {
        try {
            // Extract current user ID from JWT
            Long currentUserId = jwtUtil.extractUserId(token);

            // Validate request
            if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("Participant IDs are required"));
            }

            // Convert participant IDs from String to Long
            List<Long> participantIds = new ArrayList<>();
            for (String participantId : request.getParticipantIds()) {
                try {
                    participantIds.add(Long.parseLong(participantId));
                } catch (NumberFormatException e) {
                    return ResponseEntity.ok(ApiResponse.error("Invalid participant ID: " + participantId));
                }
            }

            // Create conversation using service
            ConversationDto conversation = messageService.createConversation(
                    currentUserId,
                    participantIds,
                    request.getIsGroup(),
                    request.getGroupName()
            );

            return ResponseEntity.ok(ApiResponse.success("Conversation created successfully", conversation));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to create conversation: " + e.getMessage()));
        }
    }

    /**
     * Get all conversations for current user
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> getConversations(
            @RequestHeader(value = "Authorization") String token) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            System.out.println("[DEBUG] Getting conversations for userId: " + currentUserId);
            List<ConversationDto> conversations = messageService.getUserConversations(currentUserId);
            System.out.println("[DEBUG] Found " + conversations.size() + " conversations for userId: " + currentUserId);
            return ResponseEntity.ok(ApiResponse.success(conversations));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to get conversations: " + e.getMessage()));
        }
    }

    /**
     * Get conversation by ID
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String conversationId) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            return messageService.getConversationByUuid(conversationId, currentUserId)
                    .map(conv -> ResponseEntity.ok(ApiResponse.success(conv)))
                    .orElse(ResponseEntity.ok(ApiResponse.error("Conversation not found")));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to get conversation: " + e.getMessage()));
        }
    }

    /**
     * Update pinned status
     */
    @PutMapping("/conversations/{conversationId}/pin")
    public ResponseEntity<ApiResponse<ConversationDto>> updatePinnedStatus(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String conversationId,
            @RequestBody UpdatePinnedStatusRequest request) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            messageService.updatePinnedStatus(conversationId, currentUserId, request.getIsPinned());
            return ResponseEntity.ok(ApiResponse.success("Pinned status updated", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to update pinned status: " + e.getMessage()));
        }
    }

    /**
     * Mark conversation as read
     */
    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Object>> markAsRead(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String conversationId) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            messageService.markConversationAsRead(conversationId, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to mark as read: " + e.getMessage()));
        }
    }

    /**
     * Delete conversation
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<Object>> deleteConversation(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String conversationId) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            messageService.deleteConversation(conversationId, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to delete conversation: " + e.getMessage()));
        }
    }

    /**
     * Send a message in a conversation
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            MessageDto message = messageService.sendMessage(conversationId, currentUserId, request);
            return ResponseEntity.ok(ApiResponse.success("Message sent successfully", message));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to send message: " + e.getMessage()));
        }
    }

    /**
     * Get messages in a conversation with pagination
     * @param page Page number (0-indexed)
     * @param size Number of messages per page (default: 50)
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageListDto>> getMessages(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            org.springframework.data.domain.Page<MessageDto> messagesPage =
                    messageService.getMessages(conversationId, currentUserId, page, size);

            // Convert Spring Page to custom response format
            MessageListDto messageList = MessageListDto.builder()
                    .messages(messagesPage.getContent())
                    .hasMore(!messagesPage.isLast())
                    .totalCount(messagesPage.getTotalElements())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(messageList));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to get messages: " + e.getMessage()));
        }
    }

    /**
     * Delete a single message
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Object>> deleteMessage(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable Long messageId) {
        try {
            Long currentUserId = jwtUtil.extractUserId(token);
            messageService.deleteMessage(messageId, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Message deleted successfully", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Failed to delete message: " + e.getMessage()));
        }
    }
}
