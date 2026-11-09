package com.imagerecognitioner.security;

import com.imagerecognitioner.security.token.TokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests application-specific validation of Cognito access-token claims. */
class TokenValidatorTest {
    @Test
    void validatesAccessTokenUse() {
        assertFalse(TokenValidator.tokenUse().validate(jwt("access", "client-1", "tenant-1")).hasErrors());
        assertTrue(TokenValidator.tokenUse().validate(jwt("id", "client-1", "tenant-1")).hasErrors());
    }

    @Test
    void validatesConfiguredClientId() {
        assertFalse(TokenValidator.clientId(Set.of("client-1"))
                .validate(jwt("access", "client-1", "tenant-1")).hasErrors());
        assertTrue(TokenValidator.clientId(Set.of("client-1"))
                .validate(jwt("access", "other-client", "tenant-1")).hasErrors());
    }

    @Test
    void requiresAValidTenantClaim() {
        assertFalse(TokenValidator.tenant().validate(jwt("access", "client-1", "tenant-1")).hasErrors());
        assertTrue(TokenValidator.tenant().validate(jwt("access", "client-1", "invalid/tenant")).hasErrors());
        assertTrue(TokenValidator.tenant().validate(jwt("access", "client-1", null)).hasErrors());
    }

    private Jwt jwt(String tokenUse, String clientId, String tenantId) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("token_use", tokenUse)
                .claim("client_id", clientId);
        if (tenantId != null) {
            builder.claim("tenant_id", tenantId);
        }
        return builder.build();
    }
}
