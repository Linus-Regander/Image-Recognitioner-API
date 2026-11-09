package com.imagerecognitioner.security;

import com.imagerecognitioner.cli.AwsProperties;
import com.imagerecognitioner.security.model.Claims;
import com.imagerecognitioner.security.token.TokenValidator;

import java.time.Instant;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Configures stateless Cognito JWT authentication outside local development and
 * a fixed anonymous principal when local authentication is explicitly enabled.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    private static final String ERROR_MISSING_CLIENT_IDS = "Missing client ids in Cognito configuration";
    private static final String ERROR_USER_POOL_ID = "User pool ID must be configured if issuer is not set";
    private static final String COGNITO_GROUPS_CLAIM = "cognito:groups";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String LOCAL_TENANT_ID = "local-tenant";
    private static final String LOCAL_USER_ID = "local-user";
    private static final String LOCAL_ROLE = "ROLE_ANONYMOUS";

    /**
     * Builds the production security chain, requiring an authenticated JWT for
     * every request.
     *
     * @param http the HTTP security builder
     * @return the configured security filter chain
     * @throws Exception if the security chain cannot be built
     */
    @Bean
    @Profile("!local")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter())));
        return http.build();
    }

    /**
     * Builds a permissive local security chain with a stable development
     * principal.
     *
     * @param http the HTTP security builder
     * @return the configured local security filter chain
     * @throws Exception if the security chain cannot be built
     */
    @Bean
    @Profile("local")
    @ConditionalOnProperty(name = "app.security.local-auth-enabled", havingValue = "true")
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .anonymous(anonymous -> anonymous
                .principal(localPrincipal())
                .authorities(LOCAL_ROLE))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Creates a JWT decoder that validates issuer, token use, client ID, and
     * tenant claims.
     *
     * @param props Cognito and JWT security settings
     * @param aws AWS settings used to derive the Cognito issuer when necessary
     * @return the configured JWT decoder
     * @throws IllegalStateException if required Cognito configuration is missing
     */
    @Bean
    @Profile("!local")
    JwtDecoder jwtDecoder(SecurityProperties props, AwsProperties aws) {
        if (props.clientIds() == null || props.clientIds().isEmpty()) {
            throw new IllegalStateException(ERROR_MISSING_CLIENT_IDS);
        }

        String issuer = StringUtils.hasText(props.issuer()) ? props.issuer() : cognitoIssuer(props, aws);
        String jwkSetUri = StringUtils.hasText(props.jwkSetUri()) ? props.jwkSetUri()
                : issuer + "/.well-known/jwks.json";

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                TokenValidator.tokenUse(),
                TokenValidator.clientId(props.clientIds()),
                TokenValidator.tenant()));

        return decoder;
    }

    private JwtAuthenticationConverter authenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();

        authorities.setAuthoritiesClaimName(COGNITO_GROUPS_CLAIM);
        authorities.setAuthorityPrefix(ROLE_PREFIX);

        var converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authorities);

        return converter;
    }

    private String cognitoIssuer(SecurityProperties props, AwsProperties aws) {
        if (!StringUtils.hasText(props.userPoolId())) {
            throw new IllegalStateException(ERROR_USER_POOL_ID);
        }

        return "https://cognito-idp.%s.amazonaws.com/%s".formatted(aws.getRegion(), props.userPoolId());
    }

    private Jwt localPrincipal() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("local")
                .header("alg", "none")
                .subject(LOCAL_USER_ID)
                .claim(Claims.TENANT_ID, LOCAL_TENANT_ID)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofDays(365)))
                .build();
    }
}