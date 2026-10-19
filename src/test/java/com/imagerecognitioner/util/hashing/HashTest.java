package com.imagerecognitioner.util.hashing;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HashTest {
    @Test
    void hash_usesConfiguredCryptoType() throws IOException, NoSuchAlgorithmException {
        Hash sha256 = new Hash(CryptoType.SHA256, null);
        Hash sha512 = new Hash(CryptoType.SHA512, null);

        assertEquals("277089d91c0bdf4f2e6862ba7e4a07605119431f5d13f726dd352b06f1b206a9",
                sha256.hash(input("bytes")));
        assertEquals(128, sha512.hash(input("bytes")).length());
    }

    @Test
    void hash_usesCustomHashFunction() throws IOException, NoSuchAlgorithmException {
        Hash hash = new Hash(CryptoType.SHA512, stream -> "custom");

        assertEquals("custom", hash.hash(input("anything")));
    }

    @Test
    void hash_doesNotCloseProvidedStream() throws IOException, NoSuchAlgorithmException {
        TrackingInputStream inputStream = new TrackingInputStream("bytes".getBytes(StandardCharsets.UTF_8));

        new Hash(CryptoType.SHA256, null).hash(inputStream);

        assertFalse(inputStream.closed);
    }

    @Test
    void hash_rejectsNullInput() {
        Hash hash = new Hash(CryptoType.SHA256, null);

        assertThrows(NullPointerException.class, () -> hash.hash(null));
    }

    @Test
    void hash_rejectsEmptyCustomResult() {
        Hash hash = new Hash(CryptoType.SHA256, stream -> " ");

        assertThrows(IllegalStateException.class, () -> hash.hash(input("bytes")));
    }

    private ByteArrayInputStream input(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
