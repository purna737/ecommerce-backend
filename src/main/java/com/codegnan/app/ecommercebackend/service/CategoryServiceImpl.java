package com.codegnan.app.ecommercebackend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codegnan.app.ecommercebackend.dao.CategoryDao;
import com.codegnan.app.ecommercebackend.dto.CategoryResponseDto;
import com.codegnan.app.ecommercebackend.entity.Category;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

	private CategoryDao categoryDao;

	public CategoryServiceImpl(CategoryDao categoryDao) {
		this.categoryDao = categoryDao;
	}

	@Override
	public List<CategoryResponseDto> getAllCategories() {

		List<Category> categories = categoryDao.getAllCategories();

		return categories.stream()
				.map(this::convertToResponseDto)
				.collect(Collectors.toList());
	}

	private CategoryResponseDto convertToResponseDto(Category category) {

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
}