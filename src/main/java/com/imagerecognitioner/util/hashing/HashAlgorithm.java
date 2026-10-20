package com.imagerecognitioner.util.hashing;

/**
 * Enum for type of hash algorithm.
 * HashAlgorithm
 */
public enum HashAlgorithm {
    SHA256("SHA-256"),
    SHA512("SHA-512");

    private final String algorithm;

    HashAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String algorithm() {
        return algorithm;
    }
}