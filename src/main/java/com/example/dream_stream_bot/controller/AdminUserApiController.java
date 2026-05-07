package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.dto.UserDto;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserApiController {

    private final UserService userService;

    public AdminUserApiController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/{id}/balance")
    public ResponseEntity<UserDto> updateBalance(
            @PathVariable Long id,
            @Valid @RequestBody @Min(value = 0, message = "Баланс не может быть отрицательным") Long newBalance) {
        try {
            UserEntity updatedUser = userService.updateArtBalance(id, newBalance);
            return ResponseEntity.ok(UserDto.fromEntity(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
