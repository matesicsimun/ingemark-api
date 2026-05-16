package com.ingemark.products.service;

import java.math.BigDecimal;
import java.util.Currency;

public interface ExchangeRateService {

    /**
     * Converts the given EUR amount into the target currency. The implementation
     * owns the rate source and applies the target currency's default fraction
     * digits as the rounding scale.
     */
    BigDecimal convertFromEur(BigDecimal eurAmount, Currency targetCurrency);
}
