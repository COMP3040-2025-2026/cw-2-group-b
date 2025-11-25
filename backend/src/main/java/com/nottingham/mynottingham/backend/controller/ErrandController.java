package com.nottingham.mynottingham.backend.controller;

import com.nottingham.mynottingham.backend.dto.CreateErrandRequest;
import com.nottingham.mynottingham.backend.dto.ErrandDto;
import com.nottingham.mynottingham.backend.entity.Errand;
import com.nottingham.mynottingham.backend.entity.User;
import com.nottingham.mynottingham.backend.repository.ErrandRepository;
import com.nottingham.mynottingham.backend.repository.UserRepository;
import com.nottingham.mynottingham.backend.util.JwtUtil; // 引入 JwtUtil
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/errand")
@RequiredArgsConstructor
public class ErrandController {

    private final ErrandRepository errandRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil; // 注入 JwtUtil

    @GetMapping("/available")
    public ResponseEntity<List<ErrandDto>> getAvailableErrands() {
        List<Errand> allErrands = errandRepository.findAll();
        List<ErrandDto> dtos = allErrands.stream()
                .map(ErrandDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/create")
    public ResponseEntity<ErrandDto> createErrand(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateErrandRequest request) {

        // [Fix] 使用 JwtUtil 解析 Token，解决 401 问题
        String token = authHeader.replace("Bearer ", "");
        Long userId;
        try {
            userId = jwtUtil.extractUserId(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Errand errand = new Errand();
        errand.setRequester(requester);
        errand.setTitle(request.getTitle());
        errand.setDescription(request.getDescription());

        try {
            errand.setType(Errand.ErrandType.valueOf(request.getType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            errand.setType(Errand.ErrandType.OTHER);
        }

        errand.setLocation(request.getPickupLocation());
        errand.setReward(request.getFee());
        errand.setImageUrl(request.getImageUrl());
        errand.setDeadline(request.getDeadline());
        errand.setStatus(Errand.ErrandStatus.PENDING);

        Errand savedErrand = errandRepository.save(errand);
        return ResponseEntity.status(HttpStatus.CREATED).body(ErrandDto.fromEntity(savedErrand));
    }

    // [New] 接单接口
    @PostMapping("/{id}/accept")
    public ResponseEntity<ErrandDto> acceptErrand(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        String token = authHeader.replace("Bearer ", "");
        Long userId;
        try {
            userId = jwtUtil.extractUserId(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User provider = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return errandRepository.findById(id)
                .map(errand -> {
                    // 检查是否已被接单
                    if (errand.getStatus() != Errand.ErrandStatus.PENDING) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).<ErrandDto>build();
                    }
                    // 不能接自己的单
                    if (errand.getRequester().getId().equals(userId)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).<ErrandDto>build();
                    }

                    errand.setProvider(provider);
                    errand.setStatus(Errand.ErrandStatus.IN_PROGRESS);

                    Errand updated = errandRepository.save(errand);
                    return ResponseEntity.ok(ErrandDto.fromEntity(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // [New] 弃单接口 (接单者取消)
    @PostMapping("/{id}/drop")
    public ResponseEntity<ErrandDto> dropErrand(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) { // 允许空 Body

        String token = authHeader.replace("Bearer ", "");
        Long userId;
        try {
            userId = jwtUtil.extractUserId(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return errandRepository.findById(id)
                .map(errand -> {
                    // 只有接单人可以弃单
                    if (errand.getProvider() != null && errand.getProvider().getId().equals(userId)) {
                        errand.setProvider(null);
                        errand.setStatus(Errand.ErrandStatus.PENDING);
                        Errand updated = errandRepository.save(errand);
                        return ResponseEntity.ok(ErrandDto.fromEntity(updated));
                    }
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<ErrandDto>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ErrandDto>> getAllErrands() {
        List<ErrandDto> dtos = errandRepository.findAll().stream()
                .map(ErrandDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ErrandDto> getErrandById(@PathVariable Long id) {
        return errandRepository.findById(id)
                .map(ErrandDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ErrandDto> updateErrandStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> statusMap) {

        return errandRepository.findById(id)
                .map(errand -> {
                    String newStatus = statusMap.get("status");
                    if (newStatus != null) {
                        try {
                            errand.setStatus(Errand.ErrandStatus.valueOf(newStatus));
                        } catch (IllegalArgumentException e) { }
                    }
                    Errand updated = errandRepository.save(errand);
                    return ResponseEntity.ok(ErrandDto.fromEntity(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteErrand(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (errandRepository.existsById(id)) {
            errandRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ErrandDto> updateErrand(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody CreateErrandRequest request) {
        return errandRepository.findById(id)
                .map(errand -> {
                    errand.setTitle(request.getTitle());
                    errand.setDescription(request.getDescription());
                    try {
                        errand.setType(Errand.ErrandType.valueOf(request.getType().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        errand.setType(Errand.ErrandType.OTHER);
                    }
                    errand.setLocation(request.getPickupLocation());
                    errand.setReward(request.getFee());
                    errand.setImageUrl(request.getImageUrl());
                    Errand updated = errandRepository.save(errand);
                    return ResponseEntity.ok(ErrandDto.fromEntity(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}