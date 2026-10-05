package com.codegnan.app.ecommercebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public class ProductResponseDto {

	private Long productId;
	private String name;
	private String brand;
	private String description;
	private BigDecimal price;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Set<CategoryResponseDto> categories;

	public ProductResponseDto() {
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Set<CategoryResponseDto> getCategories() {
		return categories;
	}

	public void setCategories(Set<CategoryResponseDto> categories) {
		this.categories = categories;
	}
}