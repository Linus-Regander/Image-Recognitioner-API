package com.imagerecognitioner.security.token;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.imagerecognitioner.security.model.Claims;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Factory for validators enforcing application-specific Cognito access-token
 * claims.
 */
public final class TokenValidator {
    private static final Pattern TENANT_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private TokenValidator() {}

    /**
     * Creates a validator that accepts access tokens and rejects other token
     * types.
     *
     * @return a token-use validator
     */
    public static OAuth2TokenValidator<Jwt> tokenUse() {
        return jwt -> "access".equals(jwt.getClaimAsString(Claims.TOKEN_USE))
                ? OAuth2TokenValidatorResult.success()
                : failure("token_use must be 'access'");
    }

    /**
     * Creates a validator that accepts only configured Cognito app clients.
     *
     * @param allowed the set of permitted client IDs
     * @return a client ID validator
     */
    public static OAuth2TokenValidator<Jwt> clientId(Set<String> allowed) {
        return jwt -> {
            String clientId = jwt.getClaimAsString(Claims.CLIENT_ID);

            return clientId != null && allowed.contains(clientId)
                    ? OAuth2TokenValidatorResult.success()
                    : failure("Unexpected client_id");
        };
    }

    /**
     * Creates a validator that requires a tenant ID containing only letters,
     * digits, underscores, or hyphens, with a maximum length of 64 characters.
     *
     * @return a tenant-claim validator
     */
    public static OAuth2TokenValidator<Jwt> tenant() {
        return jwt -> {
            String tenant = jwt.getClaimAsString(Claims.TENANT_ID);

            return tenant != null && TENANT_ID.matcher(tenant).matches()
                    ? OAuth2TokenValidatorResult.success()
                    : failure("Missing or invalid tenant claim");
        };
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}