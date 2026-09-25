package com.shortlink.shortlink.service;

import com.shortlink.shortlink.dto.CreateUrlRequest;
import com.shortlink.shortlink.dto.UrlResponse;
import com.shortlink.shortlink.dto.UrlStatsResponse;
import com.shortlink.shortlink.entity.Click;
import com.shortlink.shortlink.entity.Url;
import com.shortlink.shortlink.repository.ClickRepository;
import com.shortlink.shortlink.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final ClickRepository clickRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public UrlService(UrlRepository urlRepository,
                      ClickRepository clickRepository,
                      RedisTemplate<String, String> redisTemplate) {
        this.urlRepository = urlRepository;
        this.clickRepository = clickRepository;
        this.redisTemplate = redisTemplate;
    }

    public UrlResponse createShortUrl(CreateUrlRequest request) {
        Url url = new Url();
        url.setOriginalUrl(request.url());
        url.setCreatedAt(Instant.now());
        url.setCode("");
        url = urlRepository.save(url);

        String code = encodeBase62(url.getId());
        url.setCode(code);
        urlRepository.save(url);

        redisTemplate.opsForValue().set(code, url.getOriginalUrl(), 24, TimeUnit.HOURS);

        return new UrlResponse(code, baseUrl + "/" + code, url.getCreatedAt());
    }

    public String getOriginalUrlAndRegisterClick(String code, String ip, String userAgent) {
        String cachedUrl = redisTemplate.opsForValue().get(code);

        Url url;
        if (cachedUrl != null) {
            url = urlRepository.findByCode(code)
                    .orElseThrow(() -> new NoSuchElementException("Código não encontrado"));
        } else {
            url = urlRepository.findByCode(code)
                    .orElseThrow(() -> new NoSuchElementException("Código não encontrado"));
            redisTemplate.opsForValue().set(code, url.getOriginalUrl(), 24, TimeUnit.HOURS);
        }

        Click click = new Click();
        click.setUrl(url);
        click.setAccessedAt(Instant.now());
        click.setIpAddress(ip);
        click.setUserAgent(userAgent);
        clickRepository.save(click);

        return url.getOriginalUrl();
    }

    public UrlStatsResponse getStats(String code) {
        Url url = urlRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Código não encontrado"));

        long totalClicks = clickRepository.countByUrl(url);

        Instant lastAccessedAt = clickRepository.findTopByUrlOrderByAccessedAtDesc(url)
                .map(Click::getAccessedAt)
                .orElse(null);

        return new UrlStatsResponse(url.getCode(), url.getOriginalUrl(), totalClicks, lastAccessedAt);
    }

    private String encodeBase62(long id) {
        StringBuilder sb = new StringBuilder();
        long value = id;
        do {
            int remainder = (int) (value % 62);
            sb.insert(0, BASE62_CHARS.charAt(remainder));
            value /= 62;
        } while (value > 0);
        return sb.toString();
    }
}