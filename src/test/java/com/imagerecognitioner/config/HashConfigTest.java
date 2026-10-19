package com.imagerecognitioner.config;

import com.imagerecognitioner.util.hashing.CryptoType;
import com.imagerecognitioner.util.hashing.HashFunc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HashConfigTest {
    @Test
    void hash_usesConfiguredCryptoTypeWhenNoCustomFunctionExists() throws Exception {
        HashProperties properties = new HashProperties();
        properties.setCryptoType(CryptoType.SHA512);
        ObjectProvider<HashFunc> hashFuncProvider = mock();
        when(hashFuncProvider.getIfAvailable()).thenReturn(null);

        String result = new HashConfig().hash(properties, hashFuncProvider)
                .hash(new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8)));

        assertEquals(128, result.length());
    }

    @Test
    void hash_prefersProvidedCustomFunction() throws Exception {
        HashProperties properties = new HashProperties();
        properties.setCryptoType(CryptoType.SHA256);
        ObjectProvider<HashFunc> hashFuncProvider = mock();
        when(hashFuncProvider.getIfAvailable()).thenReturn(inputStream -> "custom");

        String result = new HashConfig().hash(properties, hashFuncProvider)
                .hash(new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8)));

        assertEquals("custom", result);
    }
}
