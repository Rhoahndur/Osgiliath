package com.osgiliath.application.auth;

import com.osgiliath.application.auth.dto.LoginResponse;
import com.osgiliath.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHandler {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse handle(LoginCommand command) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    command.getUsername(),
                    command.getPassword()
                )
            );

            String token = jwtTokenProvider.generateToken(authentication.getName());
            log.info("User {} logged in successfully", command.getUsername());

            return new LoginResponse(token, authentication.getName());
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", command.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}
