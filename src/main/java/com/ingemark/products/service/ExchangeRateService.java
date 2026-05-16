package com.ingemark.products.service;

import java.math.BigDecimal;
import java.util.Currency;

public interface ExchangeRateService {
    BigDecimal convertFromEur(BigDecimal eurAmount, Currency targetCurrency);
}
