package com.codegnan.app.ecommercebackend.service;

import java.util.List;

import com.codegnan.app.ecommercebackend.dto.CategoryResponseDto;

public interface CategoryService {

	List<CategoryResponseDto> getAllCategories();

}