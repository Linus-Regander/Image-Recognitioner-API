package com.imagerecognitioner.util.hashing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class Hash {
    private final HashFunc hashFunc;

    public Hash(CryptoType cryptoType, HashFunc hashFunc) {
        if (hashFunc != null) {
            this.hashFunc = hashFunc;
        } else {
            Objects.requireNonNull(cryptoType, "cryptoType must not be null when hashFunc is not set");
            this.hashFunc = inputStream -> digest(cryptoType, inputStream);
        }
    }

    public String hash(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        String result = hashFunc.hash(inputStream);
        if (result == null || result.isBlank()) {
            throw new IllegalStateException("Hash function must return a non-empty hash");
        }
        return result;
    }

    private static String digest(CryptoType type, InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(type.algorithm());

        DigestInputStream dis = new DigestInputStream(inputStream, md);
        dis.transferTo(OutputStream.nullOutputStream());

        return HexFormat.of().formatHex(md.digest());
    }
}