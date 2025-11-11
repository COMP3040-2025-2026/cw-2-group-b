package com.nottingham.mynottingham.backend.dto;

import com.nottingham.mynottingham.backend.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private User user;
}
