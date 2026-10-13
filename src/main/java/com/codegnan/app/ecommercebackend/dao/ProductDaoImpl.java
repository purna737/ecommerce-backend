package com.codegnan.app.ecommercebackend.dao;

import java.util.List;
import java.time.LocalDateTime;


import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.codegnan.app.ecommercebackend.entity.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Transactional
public class ProductDaoImpl implements ProductDao {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public boolean addProduct(Product product) {

		entityManager.persist(product);

		return true;
	}

	@Override
	public List<Product> searchProductsByName(String name) {

	    String jpql =
	            "SELECT DISTINCT p FROM Product p " +
	            "LEFT JOIN FETCH p.categories " +
	            "WHERE LOWER(p.name) LIKE LOWER(:name)";

	    return entityManager
	            .createQuery(jpql, Product.class)
	            .setParameter("name", "%" + name + "%")
	            .getResultList();
	}

	@Override
	public List<Product> getAllProducts() {

		return entityManager.createQuery(
				"SELECT DISTINCT p FROM Product p " +
				"LEFT JOIN FETCH p.categories",
				Product.class)
				.getResultList();
	}

	@Override
	public boolean updateProduct(Product product) {

		entityManager.merge(product);

		return true;
	}

	@Override
	public boolean deleteProduct(Long productId) {

		Product product = entityManager.find(Product.class, productId);

		if (product != null) {
			// Preserve order/review foreign-key history by deactivating instead of hard deleting.
			product.setStatus("INACTIVE");
			product.setUpdatedAt(LocalDateTime.now());
			entityManager.merge(product);
			return true;
		}

		return false;
	}

	@Override
	public List<Product> getProductsByCategory(Long categoryId) {

		return entityManager.createQuery(
				"SELECT DISTINCT p FROM Product p " +
				"JOIN FETCH p.categories c " +
				"WHERE c.categoryId = :categoryId",
				Product.class)
				.setParameter("categoryId", categoryId)
				.getResultList();
	}

	@Override
	public Product getProductById(Long productId) {

	    String jpql =
	            "SELECT DISTINCT p FROM Product p " +
	            "LEFT JOIN FETCH p.categories " +
	            "WHERE p.productId = :productId";

	    return entityManager
	            .createQuery(jpql, Product.class)
	            .setParameter("productId", productId)
	            .getResultStream()
	            .findFirst()
	            .orElse(null);
	}
	
	}
