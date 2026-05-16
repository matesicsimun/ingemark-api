package com.ingemark.products.service;

import com.ingemark.products.exception.ExchangeRateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HnbExchangeRateService implements ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(HnbExchangeRateService.class);
    private static final String PATH = "/tecajni-eur/v3";

    private final RestClient hnbRestClient;

    public HnbExchangeRateService(RestClient hnbRestClient) {
        this.hnbRestClient = hnbRestClient;
    }

    @Override
    public BigDecimal getEurRate(String currency) {
        try {
            List<HnbExchangeRate> rates = hnbRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(PATH).queryParam("valuta", currency).build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ExchangeRateException(
                                "HNB API returned status " + res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (rates == null || rates.isEmpty()) {
                throw new ExchangeRateException("HNB API returned no rates for currency " + currency);
            }

            String raw = rates.get(0).middleRate();
            if (raw == null || raw.isBlank()) {
                throw new ExchangeRateException("HNB API returned empty middle rate for " + currency);
            }

            // HNB returns numbers in Croatian locale (comma as decimal separator).
            return new BigDecimal(raw.replace(',', '.'));
        } catch (ExchangeRateException e) {
            throw e;
        } catch (RestClientException | NumberFormatException e) {
            log.warn("Failed to fetch exchange rate from HNB for {}", currency, e);
            throw new ExchangeRateException("Failed to fetch exchange rate from HNB", e);
        }
    }
}
