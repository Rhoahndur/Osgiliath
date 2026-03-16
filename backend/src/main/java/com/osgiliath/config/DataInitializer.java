package com.osgiliath.config;

import com.osgiliath.domain.auth.User;
import com.osgiliath.infrastructure.auth.JpaUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default data on application startup.
 * Disabled in production via app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.seed.admin-email:admin@osgiliath.com}")
    private String adminEmail;

    public DataInitializer(JpaUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User(
                adminUsername,
                passwordEncoder.encode(adminPassword),
                adminEmail
            );
            userRepository.save(admin);
            log.info("Default admin user created: {}", adminUsername);
        } else {
            log.info("Admin user already exists");
        }
    }
}
