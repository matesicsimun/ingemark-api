package com.ingemark.products.mapper;

import com.ingemark.products.dto.CreateProductRequest;
import com.ingemark.products.dto.ProductResponse;
import com.ingemark.products.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest request, BigDecimal priceUsd) {
        return new Product(
                request.code(),
                request.name(),
                request.priceEur(),
                priceUsd,
                Boolean.TRUE.equals(request.isAvailable())
        );
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getPriceEur(),
                product.getPriceUsd(),
                product.isAvailable()
        );
    }
}
