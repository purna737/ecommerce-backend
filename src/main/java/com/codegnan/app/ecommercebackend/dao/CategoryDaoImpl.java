package com.codegnan.app.ecommercebackend.dao;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.codegnan.app.ecommercebackend.entity.Category;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Transactional
public class CategoryDaoImpl implements CategoryDao {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Category> getAllCategories() {

		return entityManager
				.createQuery(
						"SELECT c FROM Category c WHERE c.active = true",
						Category.class)
				.getResultList();
	}
}