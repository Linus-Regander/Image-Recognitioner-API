package com.imagerecognitioner.security.model;

/**
 * Names of the Cognito claims consumed by the application.
 */
public final class Claims {
    /** Claim containing the tenant that owns the authenticated request. */
    public static final String TENANT_ID = "tenant_id";
    /** Claim containing the Cognito app client ID. */
    public static final String CLIENT_ID = "client_id";
    /** Claim identifying the kind of Cognito token. */
    public static final String TOKEN_USE = "token_use";

    private Claims() {}
}