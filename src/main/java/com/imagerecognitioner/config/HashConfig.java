package com.imagerecognitioner.config;

import com.imagerecognitioner.util.hashing.Hash;
import com.imagerecognitioner.util.hashing.HashFunc;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the application's hash service.
 */
@Configuration
@EnableConfigurationProperties(HashProperties.class)
public class HashConfig {
    /**
     * Creates the hash service using the configured algorithm or an available custom hash function.
     * @param hashProperties The hash configuration properties.
     * @param hashFuncProvider The provider for an optional custom hash function.
     * @return The configured hash service.
     */
    @Bean
    public Hash hash(HashProperties hashProperties, ObjectProvider<HashFunc> hashFuncProvider) {
        return new Hash(hashProperties.getHashAlgorithm(), hashFuncProvider.getIfAvailable());
    }
}
