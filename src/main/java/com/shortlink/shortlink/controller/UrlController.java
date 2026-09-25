package com.shortlink.shortlink.controller;

import com.shortlink.shortlink.dto.CreateUrlRequest;
import com.shortlink.shortlink.dto.UrlResponse;
import com.shortlink.shortlink.dto.UrlStatsResponse;
import com.shortlink.shortlink.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/urls")
    public ResponseEntity<UrlResponse> createUrl(@Valid @RequestBody CreateUrlRequest request) {
        UrlResponse response = urlService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = urlService.getOriginalUrlAndRegisterClick(
                code,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }

    @GetMapping("/urls/{code}/stats")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable String code) {
        UrlStatsResponse stats = urlService.getStats(code);
        return ResponseEntity.ok(stats);
    }
}