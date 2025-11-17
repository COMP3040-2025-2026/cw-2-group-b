package com.nottingham.mynottingham.backend.util;

import com.nottingham.mynottingham.backend.entity.Student;
import com.nottingham.mynottingham.backend.entity.Teacher;
import com.nottingham.mynottingham.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // Secret key for signing tokens (in production, use environment variable)
    private static final String SECRET_KEY = "MyNottinghamSecretKeyForJWTTokenGeneration2024ThisIsAVeryLongSecretKey";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

    private final SecretKey key;

    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Generate JWT token for a user
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());

        // Add student-specific claims
        if (user instanceof Student) {
            Student student = (Student) user;
            claims.put("studentId", student.getStudentId());
            claims.put("faculty", student.getFaculty());
            claims.put("major", student.getMajor());
            claims.put("yearOfStudy", student.getYearOfStudy());
        }

        // Add teacher-specific claims
        if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            claims.put("employeeId", teacher.getEmployeeId());
            claims.put("department", teacher.getDepartment());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        // Remove "Bearer " prefix if present
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        }
        return (Long) userIdObj;
    }

    /**
     * Get user ID from token (alias for extractUserId)
     */
    public Long getUserIdFromToken(String token) {
        return extractUserId(token);
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract role from token
     */
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    /**
     * Extract faculty from token (for students)
     */
    public String extractFaculty(String token) {
        return (String) extractAllClaims(token).get("faculty");
    }

    /**
     * Extract department from token (for teachers)
     */
    public String extractDepartment(String token) {
        return (String) extractAllClaims(token).get("department");
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
}
