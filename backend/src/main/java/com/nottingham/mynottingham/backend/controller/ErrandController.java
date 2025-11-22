package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.CreateErrandRequest;
import com.nottingham.mynottingham.backend.dto.ErrandDto;
import com.nottingham.mynottingham.backend.entity.Errand;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.repository.ErrandRepository;
import com.nottingham.mynottingham.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/errand")
@RequiredArgsConstructor
public class ErrandController {

    private final ErrandRepository errandRepository;
    private final UserRepository userRepository;

    /**
     * Get all errands (for My Tasks page)
     * Returns all errands - frontend filters by user
     */
    @GetMapping("/available")
    public ResponseEntity<List<ErrandDto>> getAvailableErrands() {
        List<Errand> allErrands = errandRepository.findAll();
        List<ErrandDto> dtos = allErrands.stream()
                .map(ErrandDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Create a new errand
     */
    @PostMapping("/create")
    public ResponseEntity<ErrandDto> createErrand(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateErrandRequest request) {

        // Extract user ID from token (simplified - assumes format "Bearer {userId}")
        String token = authHeader.replace("Bearer ", "");
        Long userId;
        try {
            userId = Long.parseLong(token);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Errand errand = new Errand();
        errand.setRequester(requester);
        errand.setTitle(request.getTitle());
        errand.setDescription(request.getDescription());

        // Map type string to enum
        try {
            errand.setType(Errand.ErrandType.valueOf(request.getType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            errand.setType(Errand.ErrandType.OTHER);
        }

        // Use pickupLocation as location
        errand.setLocation(request.getPickupLocation());
        errand.setReward(request.getFee());
        errand.setImageUrl(request.getImageUrl());
        errand.setStatus(Errand.ErrandStatus.PENDING);

        Errand savedErrand = errandRepository.save(errand);
        return ResponseEntity.status(HttpStatus.CREATED).body(ErrandDto.fromEntity(savedErrand));
    }

    /**
     * Get all errands
     */
    @GetMapping
    public ResponseEntity<List<ErrandDto>> getAllErrands() {
        List<ErrandDto> dtos = errandRepository.findAll().stream()
                .map(ErrandDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get errand by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ErrandDto> getErrandById(@PathVariable Long id) {
        return errandRepository.findById(id)
                .map(ErrandDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
