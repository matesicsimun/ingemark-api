package com.ingemark.products.service;

import com.ingemark.products.exception.ExchangeRateException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

@Service
public class HnbExchangeRateService implements ExchangeRateService {

    private static final String PATH = "/tecajn-eur/v3";
    public static final String VALUTA_PARAM = "valuta";
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final RestClient hnbRestClient;

    public HnbExchangeRateService(RestClient hnbRestClient) {
        this.hnbRestClient = hnbRestClient;
    }

    @Override
    public BigDecimal convertFromEur(BigDecimal eurAmount, Currency targetCurrency) {
        BigDecimal rate = fetchMiddleRate(targetCurrency);
        return eurAmount.multiply(rate).setScale(targetCurrency.getDefaultFractionDigits(), ROUNDING);
    }

    private BigDecimal fetchMiddleRate(Currency currency) {
        String code = currency.getCurrencyCode();
        List<HnbExchangeRate> rates = fetchExchangeRates(code);

        if (rates == null || rates.isEmpty()) {
            throw new ExchangeRateException("HNB API returned no rates for currency " + code);
        }

        String raw = rates.get(0).middleRate();
        if (raw == null || raw.isBlank()) {
            throw new ExchangeRateException("HNB API returned empty middle rate for " + code);
        }

        return parseRate(raw, code);
    }

    private List<HnbExchangeRate> fetchExchangeRates(String code) {
        try {
            return hnbRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(PATH).queryParam(VALUTA_PARAM, code).build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ExchangeRateException(
                                "HNB API returned status " + res.getStatusCode() + " for " + code);
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            throw new ExchangeRateException("HNB request failed for " + code, e);
        }
    }

    // HNB API returns numbers in Croatian locale (comma as decimal separator).
    private BigDecimal parseRate(String raw, String code) {
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ExchangeRateException(
                    "Invalid middle rate '" + raw + "' from HNB for " + code, e);
        }
    }
}
