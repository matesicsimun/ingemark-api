package com.ingemark.products.service;

import com.ingemark.products.config.HnbProperties;
import com.ingemark.products.dto.CreateProductRequest;
import com.ingemark.products.dto.ProductResponse;
import com.ingemark.products.entity.Product;
import com.ingemark.products.exception.DuplicateProductCodeException;
import com.ingemark.products.exception.ProductNotFoundException;
import com.ingemark.products.mapper.ProductMapper;
import com.ingemark.products.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final ExchangeRateService exchangeRateService;
    private final ProductMapper mapper;
    private final HnbProperties hnbProperties;

    public ProductService(ProductRepository repository,
                          ExchangeRateService exchangeRateService,
                          ProductMapper mapper,
                          HnbProperties hnbProperties) {
        this.repository = repository;
        this.exchangeRateService = exchangeRateService;
        this.mapper = mapper;
        this.hnbProperties = hnbProperties;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new DuplicateProductCodeException(request.code());
        }

        BigDecimal rate = exchangeRateService.getEurRate(hnbProperties.currency());
        BigDecimal priceUsd = request.priceEur()
                .multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);

        Product product = mapper.toEntity(request, priceUsd);

        try {
            return mapper.toResponse(repository.saveAndFlush(product));
        } catch (DataIntegrityViolationException e) {
            // Defensive guard against race condition on the unique code constraint.
            throw new DuplicateProductCodeException(request.code());
        }
    }

    public ProductResponse findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public List<ProductResponse> findAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }
}
