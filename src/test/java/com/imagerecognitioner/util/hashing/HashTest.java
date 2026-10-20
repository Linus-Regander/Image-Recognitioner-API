package com.imagerecognitioner.util.hashing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HashTest {
    @Test
    void hash_handlesNamedScenarios() {
        Map<String, Executable> scenarios = new LinkedHashMap<>();
        scenarios.put("computes SHA-256", () -> assertEquals(
                "277089d91c0bdf4f2e6862ba7e4a07605119431f5d13f726dd352b06f1b206a9",
                new Hash(HashAlgorithm.SHA256, null).hash(input("bytes"))));
        scenarios.put("computes SHA-512", () -> assertEquals(
                "ad87abc1efb42e03207f3a98b8f411dad0953aa23d4d319a424880ce705a12191ee2c7370c224e2d8a681b875cb6afa461bdb99a8693b2ddb74b25df00020d4d",
                new Hash(HashAlgorithm.SHA512, null).hash(input("bytes"))));
        scenarios.put("uses custom hash function", () -> assertEquals(
                "custom", new Hash(null, stream -> "custom").hash(input("anything"))));
        scenarios.put("does not close provided stream", () -> {
            TrackingInputStream inputStream = new TrackingInputStream("bytes".getBytes(StandardCharsets.UTF_8));
            new Hash(HashAlgorithm.SHA256, null).hash(inputStream);
            assertFalse(inputStream.closed);
        });
        scenarios.put("rejects null input stream", () -> assertThrows(
                NullPointerException.class, () -> new Hash(HashAlgorithm.SHA256, null).hash(null)));
        scenarios.put("rejects null custom result", () -> assertThrows(
                IllegalStateException.class, () -> new Hash(null, stream -> null).hash(input("bytes"))));
        scenarios.put("rejects blank custom result", () -> assertThrows(
                IllegalStateException.class, () -> new Hash(null, stream -> " ").hash(input("bytes"))));

        scenarios.forEach((scenario, testCase) -> assertDoesNotThrow(testCase, scenario));
    }

    @Test
    void constructor_rejectsNullAlgorithmWithoutCustomFunction() {
        Map<String, Executable> scenarios = new LinkedHashMap<>();
        scenarios.put("null algorithm and null custom function", () -> assertThrows(
                NullPointerException.class, () -> new Hash(null, null)));

        scenarios.forEach((scenario, testCase) -> assertDoesNotThrow(testCase, scenario));
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
