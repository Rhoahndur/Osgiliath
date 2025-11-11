package com.osgiliath.application.auth;

import com.osgiliath.application.auth.dto.UserResponse;
import com.osgiliath.domain.auth.User;
import com.osgiliath.infrastructure.auth.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterHandler {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse handle(RegisterCommand command) {
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (command.getEmail() != null && userRepository.existsByEmail(command.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User(
            command.getUsername(),
            passwordEncoder.encode(command.getPassword()),
            command.getEmail()
        );

        User savedUser = userRepository.save(user);
        log.info("User {} registered successfully", savedUser.getUsername());

        return new UserResponse(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail()
        );
    }
}
