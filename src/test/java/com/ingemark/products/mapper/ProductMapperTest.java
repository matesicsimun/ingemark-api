package com.ingemark.products.mapper;

import com.ingemark.products.dto.CreateProductRequest;
import com.ingemark.products.dto.ProductResponse;
import com.ingemark.products.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toEntity_mapsAllFieldsAndAppliesProvidedUsdPrice() {
        CreateProductRequest request = new CreateProductRequest(
                "ABC1234567",
                "Widget",
                new BigDecimal("100.00"),
                true
        );

        Product product = mapper.toEntity(request, new BigDecimal("108.50"));

        assertThat(product.getId()).isNull();
        assertThat(product.getCode()).isEqualTo("ABC1234567");
        assertThat(product.getName()).isEqualTo("Widget");
        assertThat(product.getPriceEur()).isEqualByComparingTo("100.00");
        assertThat(product.getPriceUsd()).isEqualByComparingTo("108.50");
        assertThat(product.isAvailable()).isTrue();
    }

    @Test
    void toEntity_treatsNullIsAvailableAsFalse() {
        CreateProductRequest request = new CreateProductRequest(
                "ABC1234567",
                "Widget",
                new BigDecimal("100.00"),
                null
        );

        Product product = mapper.toEntity(request, new BigDecimal("108.50"));

        assertThat(product.isAvailable()).isFalse();
    }

    @Test
    void toEntity_treatsFalseIsAvailableAsFalse() {
        CreateProductRequest request = new CreateProductRequest(
                "ABC1234567",
                "Widget",
                new BigDecimal("100.00"),
                false
        );

        Product product = mapper.toEntity(request, new BigDecimal("108.50"));

        assertThat(product.isAvailable()).isFalse();
    }

    @Test
    void toResponse_mapsAllFields() {
        Product product = new Product(
                "ABC1234567",
                "Widget",
                new BigDecimal("100.00"),
                new BigDecimal("108.50"),
                true
        );

        ProductResponse response = mapper.toResponse(product);

        assertThat(response.id()).isEqualTo(product.getId());
        assertThat(response.code()).isEqualTo("ABC1234567");
        assertThat(response.name()).isEqualTo("Widget");
        assertThat(response.priceEur()).isEqualByComparingTo("100.00");
        assertThat(response.priceUsd()).isEqualByComparingTo("108.50");
        assertThat(response.isAvailable()).isTrue();
    }
}
