package com.osgiliath;

import com.osgiliath.config.TestSecurityConfig;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.payment.PaymentRepository;
import com.osgiliath.domain.auth.UserRepository;

/**
 * Base class for integration tests using TestContainers with PostgreSQL
 * Provides common setup and utilities for all integration tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
public abstract class BaseIntegrationTest {

    @Container
    protected static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("osgiliath_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected InvoiceRepository invoiceRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TestDataBuilder testDataBuilder;

    @BeforeEach
    void baseSetup() {
        // Clean up data before each test
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
    }
}
