package com.codegnan.app.ecommercebackend.dao;

import java.util.List;

import com.codegnan.app.ecommercebackend.entity.Product;

public interface ProductDao {

    boolean addProduct(Product product);

    List<Product> searchProductsByName(String name);

    List<Product> getAllProducts();

    boolean updateProduct(Product product);
    
    Product getProductById(Long productId);

    boolean deleteProduct(Long productId);

    List<Product> getProductsByCategory(Long categoryId);
}