package com.isli.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public class EventSearchCriteria {
    private String city;
    private Long categoryId;
    private LocalDateTime from;
    private LocalDateTime to;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    public EventSearchCriteria() {}

    public EventSearchCriteria(String city, Long categoryId, LocalDateTime from, LocalDateTime to, BigDecimal minPrice, BigDecimal maxPrice) {
        this.city = city;
        this.categoryId = categoryId;
        this.from = from;
        this.to = to;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public void setFrom(LocalDateTime from) {
        this.from = from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public void setTo(LocalDateTime to) {
        this.to = to;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }
}
