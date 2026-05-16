package com.ingemark.products.service;

import com.ingemark.products.exception.ExchangeRateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HnbExchangeRateServiceTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");
    private static final String BASE_URL = "https://api.hnb.example";

    private MockRestServiceServer server;
    private HnbExchangeRateService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        service = new HnbExchangeRateService(builder.build());
    }

    @Test
    void convertFromEur_returnsConvertedAmountWithCurrencyDefaultScale() {
        expectRatesRequest("USD")
                .andRespond(withSuccess(
                        "[{\"valuta\":\"USD\",\"srednji_tecaj\":\"1,085000\"}]",
                        MediaType.APPLICATION_JSON));

        BigDecimal result = service.convertFromEur(new BigDecimal("100.00"), USD);

        assertThat(result).isEqualByComparingTo("108.50");
        assertThat(result.scale()).isEqualTo(USD.getDefaultFractionDigits()); // 2
        server.verify();
    }

    @Test
    void convertFromEur_parsesCroatianCommaDecimalSeparator() {
        expectRatesRequest("USD")
                .andRespond(withSuccess(
                        "[{\"valuta\":\"USD\",\"srednji_tecaj\":\"1,234567\"}]",
                        MediaType.APPLICATION_JSON));

        BigDecimal result = service.convertFromEur(new BigDecimal("10.00"), USD);

        assertThat(result).isEqualByComparingTo("12.35");
    }

    @Test
    void convertFromEur_appliesHalfUpRoundingForCurrencyWithZeroFractionDigits() {
        expectRatesRequest("JPY")
                .andRespond(withSuccess(
                        "[{\"valuta\":\"JPY\",\"srednji_tecaj\":\"163,55\"}]",
                        MediaType.APPLICATION_JSON));

        BigDecimal result = service.convertFromEur(new BigDecimal("1.00"), JPY);

        assertThat(result).isEqualByComparingTo("164");
        assertThat(result.scale()).isEqualTo(0);
    }

    @Test
    void convertFromEur_throwsWhenHnbReturnsServerError() {
        expectRatesRequest("USD").andRespond(withServerError());

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class);
    }

    @Test
    void convertFromEur_throwsWhenHnbReturnsNotFound() {
        expectRatesRequest("USD").andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class);
    }

    @Test
    void convertFromEur_throwsWhenHnbIsUnreachable() {
        expectRatesRequest("USD").andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class);
    }

    @Test
    void convertFromEur_throwsWhenResponseBodyIsEmptyArray() {
        expectRatesRequest("USD")
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class);
    }

    @Test
    void convertFromEur_throwsWhenResponseBodyIsNull() {
        expectRatesRequest("USD")
                .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class);
    }

    @Test
    void convertFromEur_throwsWhenMiddleRateIsNull() {
        expectRatesRequest("USD")
                .andRespond(withSuccess(
                        "[{\"valuta\":\"USD\",\"srednji_tecaj\":null}]",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class);
    }

    @Test
    void convertFromEur_throwsWhenMiddleRateIsBlank() {
        expectRatesRequest("USD")
                .andRespond(withSuccess(
                        "[{\"valuta\":\"USD\",\"srednji_tecaj\":\"   \"}]",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class);
    }

    @Test
    void convertFromEur_throwsWhenMiddleRateIsNotANumber() {
        expectRatesRequest("USD")
                .andRespond(withSuccess(
                        "[{\"valuta\":\"USD\",\"srednji_tecaj\":\"not-a-number\"}]",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.convertFromEur(new BigDecimal("100.00"), USD))
                .isInstanceOf(ExchangeRateException.class)
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    private ResponseActions expectRatesRequest(String currencyCode) {
        return server.expect(requestToUriTemplate(BASE_URL + "/tecajn-eur/v3?valuta={code}", currencyCode))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(queryParam(HnbExchangeRateService.VALUTA_PARAM, currencyCode));
    }
}
