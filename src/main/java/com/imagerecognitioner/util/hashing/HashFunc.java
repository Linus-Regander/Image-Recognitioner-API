package com.imagerecognitioner.util.hashing;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

/**
 * Defines a function that calculates a hash from an input stream.
 */
@FunctionalInterface
public interface HashFunc {
    /**
     * Calculates a hash from the supplied input stream.
     * @param inputStream The stream to hash.
     * @return The calculated hash.
     * @throws IOException If reading the input stream fails.
     * @throws NoSuchAlgorithmException If the configured algorithm is unavailable.
     */
    String hash(InputStream inputStream) throws IOException, NoSuchAlgorithmException;
}