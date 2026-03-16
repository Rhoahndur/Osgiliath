package com.osgiliath.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Test Security Configuration Disables security for integration tests to simplify testing */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public MockMvcBuilderCustomizer contextPathCustomizer(
            @Value("${server.servlet.context-path:}") String contextPath) {
        return builder -> {
            if (!contextPath.isEmpty()) {
                builder.defaultRequest(MockMvcRequestBuilders.get("/").contextPath(contextPath));
            }
        };
    }
}
