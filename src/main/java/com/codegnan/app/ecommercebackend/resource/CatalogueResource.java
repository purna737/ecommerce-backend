package com.codegnan.app.ecommercebackend.resource;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codegnan.app.ecommercebackend.dto.CategoryResponseDto;
import com.codegnan.app.ecommercebackend.dto.ProductRequestDto;
import com.codegnan.app.ecommercebackend.dto.ProductResponseDto;
import com.codegnan.app.ecommercebackend.service.AuthenticationService;
import com.codegnan.app.ecommercebackend.service.CategoryService;
import com.codegnan.app.ecommercebackend.service.ProductService;

@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5500",
		"http://127.0.0.1:5500" })
@RestController
@RequestMapping("/rest/api/v1/catalogues")
public class CatalogueResource {

	private ProductService productService;
	private CategoryService categoryService;
	private AuthenticationService authenticationService;

	public CatalogueResource(ProductService productService, CategoryService categoryService,
			AuthenticationService authenticationService) {

		this.productService = productService;
		this.categoryService = categoryService;
		this.authenticationService = authenticationService;
	}

	// =========================================================
	// ADMIN ONLY - ADD PRODUCT
	// =========================================================

	@PostMapping
	public String addProductOperation(@RequestParam Long userId, @ModelAttribute ProductRequestDto productRequestDto) {

		if (!authenticationService.isAdmin(userId)) {
			return "Access denied. Admin only.";
		}

		productService.addProduct(productRequestDto);

		return "success";
	}

	// =========================================================
	// CUSTOMER + ADMIN - SEARCH PRODUCTS
	// =========================================================

	@GetMapping("/search")
	public List<ProductResponseDto> searchProductsByNameOperation(@RequestParam String name) {

		return productService.searchProductsByName(name);
	}

	// =========================================================
	// CUSTOMER + ADMIN - VIEW ALL PRODUCTS
	// =========================================================

	@GetMapping("/viewall")
	public List<ProductResponseDto> getAllProductsOperation() {

		return productService.getAllProducts();
	}

	// =========================================================
	// CUSTOMER + ADMIN - VIEW CATEGORIES
	// =========================================================

	@GetMapping("/categories")
	public List<CategoryResponseDto> getCategoriesOperation() {

		return categoryService.getAllCategories();
	}

	// =========================================================
	// CUSTOMER + ADMIN - PRODUCTS BY CATEGORY
	// =========================================================

	@GetMapping("/categories/{categoryId}/products")
	public List<ProductResponseDto> getProductsByCategoryOperation(@PathVariable long categoryId) {

		return productService.getProductsByCategory(categoryId);
	}

	// =========================================================
	// ADMIN ONLY - UPDATE PRODUCT
	// =========================================================

	@PutMapping
	public String updateProductNameOperation(@RequestParam Long userId, @RequestParam long productId,
			@RequestParam String updatedName) {

		if (!authenticationService.isAdmin(userId)) {
			return "Access denied. Admin only.";
		}

		ProductRequestDto productRequestDto = new ProductRequestDto();

		productRequestDto.setName(updatedName);

		productService.updateProduct(productId, productRequestDto);

		return "success";
	}

	// =========================================================
	// ADMIN ONLY - DELETE PRODUCT
	// =========================================================

	@DeleteMapping
	public String removeProductOperation(@RequestParam Long userId, @RequestParam long productId) {

		if (!authenticationService.isAdmin(userId)) {
			return "Access denied. Admin only.";
		}

		productService.deleteProduct(productId);

		return "success";
	}
}