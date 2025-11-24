package com.nottingham.mynottingham.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private String content;
    private String messageType; // TEXT, IMAGE, FILE
    private String attachmentUrl; // Optional, for images/files
}
