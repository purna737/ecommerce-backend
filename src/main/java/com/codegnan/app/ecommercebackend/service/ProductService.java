package com.codegnan.app.ecommercebackend.service;

import java.util.List;

import com.codegnan.app.ecommercebackend.dto.ProductRequestDto;
import com.codegnan.app.ecommercebackend.dto.ProductResponseDto;

public interface ProductService {

	boolean addProduct(ProductRequestDto productRequestDto);

	List<ProductResponseDto> searchProductsByName(String name);
	
	List<ProductResponseDto> getAllProducts();

	boolean updateProduct(Long productId, ProductRequestDto productRequestDto);

	boolean deleteProduct(Long productId);

	List<ProductResponseDto> getProductsByCategory(Long categoryId);

}