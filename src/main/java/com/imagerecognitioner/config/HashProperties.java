package com.imagerecognitioner.config;

import com.imagerecognitioner.util.hashing.HashAlgorithm;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for hash algorithm selection.
 */
@ConfigurationProperties(prefix = "app.hash")
public class HashProperties {
    private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA256;

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
}
