package com.ingemark.products.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "code is required")
        @Size(min = 10, max = 10, message = "code must be exactly 10 characters")
        String code,

        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "price_eur is required")
        @DecimalMin(value = "0.0", message = "price_eur must be >= 0")
        @JsonProperty("price_eur")
        BigDecimal priceEur,

        @NotNull(message = "is_available is required")
        @JsonProperty("is_available")
        Boolean isAvailable
) {
}
