package com.osgiliath;

import com.osgiliath.config.TestSecurityConfig;
import com.osgiliath.domain.auth.UserRepository;
import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.payment.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using TestContainers with PostgreSQL Provides common setup and
 * utilities for all integration tests.
 *
 * <p>When SPRING_DATASOURCE_URL is set (e.g. in CI), uses the externally-provided database.
 * Otherwise starts a Testcontainers PostgreSQL container for local development.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
public abstract class BaseIntegrationTest {

    private static final PostgreSQLContainer<?> postgresContainer;

    static {
        if (System.getenv("SPRING_DATASOURCE_URL") == null) {
            postgresContainer =
                    new PostgreSQLContainer<>("postgres:15-alpine")
                            .withDatabaseName("osgiliath_test")
                            .withUsername("test")
                            .withPassword("test");
            postgresContainer.start();
        } else {
            postgresContainer = null;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgresContainer != null) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgresContainer::getUsername);
            registry.add("spring.datasource.password", postgresContainer::getPassword);
        }
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired protected MockMvc mockMvc;

    @Autowired protected CustomerRepository customerRepository;

    @Autowired protected InvoiceRepository invoiceRepository;

    @Autowired protected PaymentRepository paymentRepository;

    @Autowired protected UserRepository userRepository;

    @Autowired protected TestDataBuilder testDataBuilder;

    @BeforeEach
    void baseSetup() {
        // Clean up data before each test
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
    }
}
