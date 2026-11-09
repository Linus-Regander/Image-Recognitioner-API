package com.imagerecognitioner.security;

import com.imagerecognitioner.cli.AwsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests required and optional settings for the production JWT decoder. */
class SecurityConfigTest {
    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void jwtDecoderRequiresConfiguredClientIds() {
        SecurityProperties properties = new SecurityProperties(null, null, null, Set.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> securityConfig.jwtDecoder(properties, awsProperties()));

        assertEquals("Missing client ids in Cognito configuration", exception.getMessage());
    }

    @Test
    void jwtDecoderRequiresUserPoolWhenIssuerIsNotConfigured() {
        SecurityProperties properties = new SecurityProperties(null, null, null, Set.of("client-1"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> securityConfig.jwtDecoder(properties, awsProperties()));

        assertEquals("User pool ID must be configured if issuer is not set", exception.getMessage());
    }

    @Test
    void jwtDecoderAcceptsExplicitIssuerWithoutUserPoolId() {
        SecurityProperties properties = new SecurityProperties(
                null,
                "https://issuer.example",
                "https://issuer.example/.well-known/jwks.json",
                Set.of("client-1"));

        JwtDecoder decoder = securityConfig.jwtDecoder(properties, awsProperties());

        assertNotNull(decoder);
    }

    private AwsProperties awsProperties() {
        AwsProperties properties = new AwsProperties();
        properties.setRegion("eu-north-1");
        return properties;
    }
}
