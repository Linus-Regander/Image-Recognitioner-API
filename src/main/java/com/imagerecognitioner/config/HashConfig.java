package com.imagerecognitioner.config;

import com.imagerecognitioner.util.hashing.Hash;
import com.imagerecognitioner.util.hashing.HashFunc;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HashProperties.class)
public class HashConfig {
    @Bean
    public Hash hash(HashProperties hashProperties, ObjectProvider<HashFunc> hashFuncProvider) {
        return new Hash(hashProperties.getCryptoType(), hashFuncProvider.getIfAvailable());
    }
}
