package com.codegnan.app.ecommercebackend.dao;

import org.springframework.stereotype.Repository;

import com.codegnan.app.ecommercebackend.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public class UserDaoImpl implements UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean saveUser(User user) {

        try {
            entityManager.persist(user);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public User getUserById(Long userId) {

        return entityManager.find(User.class, userId);
    }

    @Override
    public User getUserByEmail(String email) {

        String jpql =
                "SELECT u FROM User u " +
                "WHERE LOWER(u.email) = LOWER(:email)";

        return entityManager
                .createQuery(jpql, User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public User updateUser(User user) {

        try {
            return entityManager.merge(user);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}