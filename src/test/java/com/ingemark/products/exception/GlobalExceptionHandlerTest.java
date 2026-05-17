package com.ingemark.products.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/products";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn(PATH);
    }

    @Test
    void handleNotFound_returns404WithExceptionMessage() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ProductNotFoundException(42L), request);

        assertBody(response, HttpStatus.NOT_FOUND, "Product not found: 42");
        assertThat(response.getBody().violations()).isNull();
    }

    @Test
    void handleDuplicate_returns409WithExceptionMessage() {
        ResponseEntity<ApiError> response = handler.handleDuplicate(
                new DuplicateProductCodeException("ABC1234567"), request);

        assertBody(response, HttpStatus.CONFLICT, "Product with code 'ABC1234567' already exists");
    }

    @Test
    void handleMethodArgumentNotValid_returns400WithOneViolationPerFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("req", "code", "code must be exactly 10 characters"),
                new FieldError("req", "price_eur", "price_eur must be >= 0")
        ));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI(PATH);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(servletRequest));

        ApiError body = (ApiError) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Validation failed");
        assertThat(body.path()).isEqualTo(PATH);
        assertThat(body.violations())
                .extracting(ApiError.FieldViolation::field, ApiError.FieldViolation::message)
                .containsExactly(
                        tuple("code", "code must be exactly 10 characters"),
                        tuple("price_eur", "price_eur must be >= 0"));
    }

    @Test
    void handleExchangeRate_returns503AndDoesNotLeakInternalExceptionMessage() {
        ResponseEntity<ApiError> response = handler.handleExchangeRate(
                new ExchangeRateException("HNB API returned status 500 for USD"), request);

        assertBody(response, HttpStatus.SERVICE_UNAVAILABLE,
                "Product creation failed due to third-party unavailability. Please try again.");
    }

    @Test
    void handleUnexpected_returns500AndDoesNotLeakInternalExceptionMessage() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(
                new RuntimeException("boom"), request);

        assertBody(response, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private void assertBody(ResponseEntity<ApiError> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(status.value());
        assertThat(body.error()).isEqualTo(status.getReasonPhrase());
        assertThat(body.message()).isEqualTo(message);
        assertThat(body.path()).isEqualTo(PATH);
        assertThat(body.timestamp()).isNotNull();
    }
}
