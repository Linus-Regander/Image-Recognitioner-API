package com.imagerecognitioner.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Configuration properties used to validate Cognito-issued access tokens.
 *
 * @param userPoolId Cognito user pool ID used to derive the issuer when unset
 * @param issuer configured token issuer, if it differs from the Cognito default
 * @param jwkSetUri configured JWK set URI, if it differs from the issuer default
 * @param clientIds allowed Cognito app client IDs
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String userPoolId,
        String issuer,
        String jwkSetUri,
        Set<String> clientIds) {

    public SecurityProperties {
        clientIds = clientIds == null ? Set.of() : Set.copyOf(clientIds);
    }
}