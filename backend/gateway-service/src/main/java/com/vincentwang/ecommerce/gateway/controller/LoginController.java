package com.vincentwang.ecommerce.gateway.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A simple passwordless identity endpoint */
@RestController
@RequestMapping("/api/v1/login")
public class LoginController {

    @PostMapping
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String username = request.username().trim();
        String normalized = username.toLowerCase(Locale.ROOT);
        UUID userId = UUID.nameUUIDFromBytes(("arcade:user:" + normalized).getBytes(StandardCharsets.UTF_8));
        return new LoginResponse(userId.toString(), username);
    }

    public record LoginRequest(@NotBlank @Size(min = 2, max = 40) String username) {}

    public record LoginResponse(String userId, String username) {}
}
