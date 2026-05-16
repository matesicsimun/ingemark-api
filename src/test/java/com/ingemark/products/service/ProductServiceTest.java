package com.ingemark.products.service;

import com.ingemark.products.dto.CreateProductRequest;
import com.ingemark.products.dto.ProductResponse;
import com.ingemark.products.entity.Product;
import com.ingemark.products.exception.DuplicateProductCodeException;
import com.ingemark.products.exception.ProductNotFoundException;
import com.ingemark.products.mapper.ProductMapper;
import com.ingemark.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ExchangeRateService exchangeRateService;

    private final ProductMapper mapper = new ProductMapper();

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository, exchangeRateService, mapper);
    }

    @Test
    void create_storesConvertedUsdPrice() {
        CreateProductRequest request = new CreateProductRequest("ABC1234567", "Widget", new BigDecimal("100.00"), true);
        when(repository.existsByCode("ABC1234567")).thenReturn(false);
        when(exchangeRateService.convertFromEur(new BigDecimal("100.00"), Currency.getInstance("USD")))
                .thenReturn(new BigDecimal("108.50"));
        when(repository.saveAndFlush(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = service.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPriceUsd()).isEqualByComparingTo("108.50");
        assertThat(response.priceUsd()).isEqualByComparingTo("108.50");
    }

    @Test
    void create_rejectsDuplicateCode() {
        CreateProductRequest request = new CreateProductRequest("ABC1234567", "Widget", new BigDecimal("100.00"), true);
        when(repository.existsByCode("ABC1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateProductCodeException.class);
    }

    @Test
    void findById_throwsWhenMissing() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
