package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.ApiResponse;
import com.nottingham.mynottingham.backend.dto.LoginRequest;
import com.nottingham.mynottingham.backend.dto.LoginResponse;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("=== Login Attempt ===");
            System.out.println("Username: " + request.getUsername());
            System.out.println("Password received: " + request.getPassword());

            User user = userService.getUserByUsername(request.getUsername())
                    .orElse(null);

            if (user == null) {
                System.out.println("User not found!");
                return ResponseEntity.ok(ApiResponse.error("User not found"));
            }

            System.out.println("User found: " + user.getUsername());
            System.out.println("Stored password hash: " + user.getPassword());

            boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
            System.out.println("Password matches: " + passwordMatches);

            if (!passwordMatches) {
                System.out.println("Password verification failed!");
                return ResponseEntity.ok(ApiResponse.error("Invalid password"));
            }

            // Generate simple token (in production, use JWT)
            String token = "Bearer " + java.util.UUID.randomUUID().toString();

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setToken(token);
            loginResponse.setUser(user);

            System.out.println("Login successful!");
            return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/generate-hash")
    public ResponseEntity<String> generateHash() {
        String password = "password123";
        String hash = passwordEncoder.encode(password);
        System.out.println("Generated hash for 'password123': " + hash);
        boolean matches = passwordEncoder.matches(password, hash);
        return ResponseEntity.ok("Hash: " + hash + "\nMatches: " + matches);
    }
}
