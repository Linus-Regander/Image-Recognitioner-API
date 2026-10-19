package com.imagerecognitioner.config;

import com.imagerecognitioner.util.hashing.CryptoType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.hash")
public class HashProperties {
    private CryptoType cryptoType = CryptoType.SHA256;

    public CryptoType getCryptoType() {
        return cryptoType;
    }

    public void setCryptoType(CryptoType cryptoType) {
        this.cryptoType = cryptoType;
    }
}
