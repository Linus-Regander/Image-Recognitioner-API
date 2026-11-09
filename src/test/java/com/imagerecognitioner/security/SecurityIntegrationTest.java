package com.imagerecognitioner.security;

import com.imagerecognitioner.cli.AwsProperties;
import com.imagerecognitioner.controller.ImageController;
import com.imagerecognitioner.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies authentication and tenant claim propagation through the web layer. */
@WebMvcTest(controllers = ImageController.class)
@Import({SecurityConfig.class, SecurityIntegrationTest.AwsPropertiesConfiguration.class})
class SecurityIntegrationTest {
    private static final String TENANT_ID = "tenant-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void protectedEndpointRejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/images/metadata"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validJwtTenantClaimIsPassedToController() throws Exception {
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt(TENANT_ID));
        when(imageService.selectAllImageMetadata(TENANT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/images/metadata")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk());

        verify(imageService).selectAllImageMetadata(TENANT_ID);
    }

    @Test
    void rejectedJwtCannotAccessProtectedEndpoint() throws Exception {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid token"));

        mockMvc.perform(get("/api/images/metadata")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    private Jwt jwt(String tenantId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject("user-1")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("token_use", "access")
                .claim("client_id", "client-1")
                .claim("tenant_id", tenantId)
                .build();
    }

    @TestConfiguration
    static class AwsPropertiesConfiguration {
        @Bean
        AwsProperties awsProperties() {
            AwsProperties properties = new AwsProperties();
            properties.setRegion("eu-north-1");
            return properties;
        }
    }
}
