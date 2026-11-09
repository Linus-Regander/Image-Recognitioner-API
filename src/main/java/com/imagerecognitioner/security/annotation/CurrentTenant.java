package com.imagerecognitioner.security.annotation;

import com.imagerecognitioner.security.model.Claims;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated principal's tenant ID into a controller parameter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "claims['" + Claims.TENANT_ID + "']")
public @interface CurrentTenant {}