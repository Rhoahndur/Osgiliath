package com.osgiliath.config;

import com.osgiliath.domain.auth.User;
import com.osgiliath.infrastructure.auth.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default data on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin user if it doesn't exist
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User(
                "admin",
                passwordEncoder.encode("admin123"),
                "admin@osgiliath.com"
            );

            User savedUser = userRepository.save(admin);
            log.info("Created admin user: admin / admin123");
        } else {
            log.info("Admin user already exists");
        }
    }
}
