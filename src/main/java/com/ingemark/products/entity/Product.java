package com.ingemark.products.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price_eur", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceEur;

    @Column(name = "price_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    protected Product() {
    }

    public Product(String code, String name, BigDecimal priceEur, BigDecimal priceUsd, boolean available) {
        this.code = code;
        this.name = name;
        this.priceEur = priceEur;
        this.priceUsd = priceUsd;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceEur() {
        return priceEur;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public boolean isAvailable() {
        return available;
    }
}
