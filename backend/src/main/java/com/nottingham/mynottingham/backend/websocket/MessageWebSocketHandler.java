package com.nottingham.mynottingham.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nottingham.mynottingham.backend.dto.MessageDto;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    // Map<userId, Set<WebSocketSession>>
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // Map<conversationId, Set<WebSocketSession>>
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> conversationSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract userId from query parameters
        String userId = extractUserId(session);
        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            System.out.println("WebSocket connection established for user: " + userId);

            // Send connection confirmation
            sendMessage(session, new WebSocketMessage("CONNECTED", "Connection established", null));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

        switch (wsMessage.getType()) {
            case "JOIN_CONVERSATION":
                handleJoinConversation(session, wsMessage);
                break;
            case "LEAVE_CONVERSATION":
                handleLeaveConversation(session, wsMessage);
                break;
            case "NEW_MESSAGE":
                handleNewMessage(session, wsMessage);
                break;
            case "TYPING":
                handleTyping(session, wsMessage);
                break;
            case "STOP_TYPING":
                handleStopTyping(session, wsMessage);
                break;
            case "MESSAGE_READ":
                handleMessageRead(session, wsMessage);
                break;
            default:
                System.out.println("Unknown message type: " + wsMessage.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }

            // Remove from all conversation sessions
            conversationSessions.values().forEach(set -> set.remove(session));

            System.out.println("WebSocket connection closed for user: " + userId);
        }
    }

    /**
     * Join a conversation to receive real-time updates
     */
    private void handleJoinConversation(WebSocketSession session, WebSocketMessage message) {
        String conversationId = (String) message.getData().get("conversationId");
        if (conversationId != null) {
            conversationSessions.computeIfAbsent(conversationId, k -> new CopyOnWriteArraySet<>()).add(session);
            System.out.println("User joined conversation: " + conversationId);
        }
    }

    /**
     * Leave a conversation
     */
    private void handleLeaveConversation(WebSocketSession session, WebSocketMessage message) {
        String conversationId = (String) message.getData().get("conversationId");
        if (conversationId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = conversationSessions.get(conversationId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    conversationSessions.remove(conversationId);
                }
            }
            System.out.println("User left conversation: " + conversationId);
        }
    }

    /**
     * Broadcast new message to all participants in conversation
     */
    private void handleNewMessage(WebSocketSession session, WebSocketMessage message) {
        String conversationId = (String) message.getData().get("conversationId");
        if (conversationId != null) {
            broadcastToConversation(conversationId, message, session);
        }
    }

    /**
     * Broadcast typing indicator
     */
    private void handleTyping(WebSocketSession session, WebSocketMessage message) {
        String conversationId = (String) message.getData().get("conversationId");
        if (conversationId != null) {
            broadcastToConversation(conversationId, message, session);
        }
    }

    /**
     * Broadcast stop typing indicator
     */
    private void handleStopTyping(WebSocketSession session, WebSocketMessage message) {
        String conversationId = (String) message.getData().get("conversationId");
        if (conversationId != null) {
            broadcastToConversation(conversationId, message, session);
        }
    }

    /**
     * Broadcast message read status
     */
    private void handleMessageRead(WebSocketSession session, WebSocketMessage message) {
        String conversationId = (String) message.getData().get("conversationId");
        if (conversationId != null) {
            broadcastToConversation(conversationId, message, session);
        }
    }

    /**
     * Broadcast message to all sessions in a conversation (except sender)
     */
    private void broadcastToConversation(String conversationId, WebSocketMessage message, WebSocketSession senderSession) {
        CopyOnWriteArraySet<WebSocketSession> sessions = conversationSessions.get(conversationId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen() && !session.equals(senderSession)) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * Send message to specific user (all their sessions)
     */
    public void sendToUser(String userId, WebSocketMessage message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * Send message to all users in a conversation
     */
    public void sendToConversation(String conversationId, WebSocketMessage message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = conversationSessions.get(conversationId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * Send message to a WebSocket session
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            System.err.println("Error sending WebSocket message: " + e.getMessage());
        }
    }

    /**
     * Extract userId from WebSocket session query parameters
     */
    private String extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("userId=")) {
                    return param.substring("userId=".length());
                }
            }
        }
        return null;
    }

    /**
     * WebSocket message structure
     */
    public static class WebSocketMessage {
        private String type;
        private String message;
        private Map<String, Object> data;

        public WebSocketMessage() {}

        public WebSocketMessage(String type, String message, Map<String, Object> data) {
            this.type = type;
            this.message = message;
            this.data = data;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}
