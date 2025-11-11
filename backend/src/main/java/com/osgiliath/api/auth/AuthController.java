package com.osgiliath.api.auth;

import com.osgiliath.application.auth.*;
import com.osgiliath.application.auth.dto.LoginRequest;
import com.osgiliath.application.auth.dto.LoginResponse;
import com.osgiliath.application.auth.dto.RegisterRequest;
import com.osgiliath.application.auth.dto.UserResponse;
import com.osgiliath.domain.auth.User;
import com.osgiliath.domain.auth.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private final LoginHandler loginHandler;
    private final RegisterHandler registerHandler;
    private final UserRepository userRepository;

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.getUsername(), request.getPassword());
        LoginResponse response = loginHandler.handle(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterCommand command = new RegisterCommand(
            request.getUsername(),
            request.getPassword(),
            request.getEmail()
        );
        UserResponse response = registerHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user",
        description = "Get currently authenticated user information",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse response = new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}
