package com.ingemark.products.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "hnb.api")
public record HnbProperties(
        String baseUrl,
        String currency,
        Duration connectTimeout,
        Duration readTimeout
) {
}
