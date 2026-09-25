package com.shortlink.shortlink.dto;

import java.time.Instant;

public record UrlStatsResponse(
        String code,
        String originalUrl,
        long totalClicks,
        Instant lastAccessedAt
) {}