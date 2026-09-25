package com.shortlink.shortlink.dto;

import jakarta.validation.constraints.NotBlank;

// Coisas que eu não sabia: o record gera automaticamente o construtor, os getters e o toString
public record CreateUrlRequest(
        @NotBlank(message = "A URL não pode estar vazia")
        String url
) {}