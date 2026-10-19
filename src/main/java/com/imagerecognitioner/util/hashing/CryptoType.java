package com.imagerecognitioner.util.hashing;

/**
 * Enum for type of crypto algorithm.
 * CryptoType
 */
public enum CryptoType {
    SHA256("SHA-256"),
    SHA512("SHA-512");

    private final String algorithm;

    CryptoType(String algorithm) {
        this.algorithm = algorithm;
    }

    public String algorithm() {
        return algorithm;
    }
}