package com.ingemark.products.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HnbExchangeRate(

        @JsonProperty("valuta")
        String currency,

        @JsonProperty("srednji_tecaj")
        String middleRate
) {
}
