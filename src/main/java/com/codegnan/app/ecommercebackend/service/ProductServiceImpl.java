package com.codegnan.app.ecommercebackend.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codegnan.app.ecommercebackend.dao.ProductDao;
import com.codegnan.app.ecommercebackend.dto.CategoryResponseDto;
import com.codegnan.app.ecommercebackend.dto.ProductRequestDto;
import com.codegnan.app.ecommercebackend.dto.ProductResponseDto;
import com.codegnan.app.ecommercebackend.entity.Category;
import com.codegnan.app.ecommercebackend.entity.Product;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

	private ProductDao productDao;

	public ProductServiceImpl(ProductDao productDao) {
		this.productDao = productDao;
	}

	@Override
	public boolean addProduct(ProductRequestDto productRequestDto) {

		Product product = new Product();

		product.setName(productRequestDto.getName());
		product.setBrand(productRequestDto.getBrand());
		product.setDescription(productRequestDto.getDescription());
		product.setPrice(productRequestDto.getPrice());

		if (productRequestDto.getStatus() == null ||
				productRequestDto.getStatus().isBlank()) {

			product.setStatus("ACTIVE");

		} else {

			product.setStatus(productRequestDto.getStatus());
		}

		LocalDateTime now = LocalDateTime.now();

		product.setCreatedAt(now);
		product.setUpdatedAt(now);

		product.setCategories(new HashSet<Category>());

		return productDao.addProduct(product);
	}

	
	@Override
	public List<ProductResponseDto> getAllProducts() {

		return productDao.getAllProducts()
				.stream()
				.map(this::convertToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public boolean deleteProduct(Long productId) {

		return productDao.deleteProduct(productId);
	}

	@Override
	public List<ProductResponseDto> getProductsByCategory(Long categoryId) {

		return productDao.getProductsByCategory(categoryId)
				.stream()
				.map(this::convertToResponseDto)
				.collect(Collectors.toList());
	}

	private ProductResponseDto convertToResponseDto(Product product) {

		ProductResponseDto dto = new ProductResponseDto();

		dto.setProductId(product.getProductId());
		dto.setName(product.getName());
		dto.setBrand(product.getBrand());
		dto.setDescription(product.getDescription());
		dto.setPrice(product.getPrice());
		dto.setStatus(product.getStatus());
		dto.setCreatedAt(product.getCreatedAt());
		dto.setUpdatedAt(product.getUpdatedAt());

		if (product.getCategories() != null) {

			dto.setCategories(
					product.getCategories()
							.stream()
							.map(this::convertCategoryToResponseDto)
							.collect(Collectors.toSet())
			);
		}

		return dto;
	}

	private CategoryResponseDto convertCategoryToResponseDto(
			Category category) {

		CategoryResponseDto dto = new CategoryResponseDto();

		dto.setCategoryId(category.getCategoryId());
		dto.setName(category.getName());
		dto.setSlug(category.getSlug());
		dto.setDescription(category.getDescription());
		dto.setActive(category.getActive());
		dto.setCreatedAt(category.getCreatedAt());
		dto.setUpdatedAt(category.getUpdatedAt());

		return dto;
	}

	@Override
	public List<ProductResponseDto> searchProductsByName(String name) {

	    List<Product> products =
	            productDao.searchProductsByName(name);

	    return products.stream()
	            .map(this::convertToResponseDto)
	            .toList();
	}

	@Override
	public boolean updateProduct(
	        Long productId,
	        ProductRequestDto productRequestDto) {

	    Product existingProduct = productDao.getProductById(productId);

	    if (existingProduct == null) {
	        return false;
	    }

	    if (productRequestDto.getName() != null) {
	        existingProduct.setName(productRequestDto.getName());
	    }

	    if (productRequestDto.getBrand() != null) {
	        existingProduct.setBrand(productRequestDto.getBrand());
	    }

	    if (productRequestDto.getDescription() != null) {
	        existingProduct.setDescription(productRequestDto.getDescription());
	    }

	    if (productRequestDto.getPrice() != null) {
	        existingProduct.setPrice(productRequestDto.getPrice());
	    }

	    if (productRequestDto.getStatus() != null) {
	        existingProduct.setStatus(productRequestDto.getStatus());
	    }

	    existingProduct.setUpdatedAt(LocalDateTime.now());

	    return productDao.updateProduct(existingProduct);
	}
}