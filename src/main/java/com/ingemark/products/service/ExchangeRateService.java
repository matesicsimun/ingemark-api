package com.ingemark.products.service;

import java.math.BigDecimal;

public interface ExchangeRateService {

    /**
     * Returns the middle rate for converting 1 EUR into the given currency.
     */
    BigDecimal getEurRate(String currency);
}
