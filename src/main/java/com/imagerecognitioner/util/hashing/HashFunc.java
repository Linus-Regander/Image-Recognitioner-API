package com.imagerecognitioner.util.hashing;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

@FunctionalInterface
public interface HashFunc {
    String hash(InputStream inputStream) throws IOException, NoSuchAlgorithmException;
}