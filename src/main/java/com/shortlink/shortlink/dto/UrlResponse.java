package com.shortlink.shortlink.dto;

import java.time.Instant;

public record UrlResponse(
        String code,
        String shortUrl,
        Instant createdAt
) {}