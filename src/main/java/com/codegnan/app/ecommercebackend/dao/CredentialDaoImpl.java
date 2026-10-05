package com.codegnan.app.ecommercebackend.dao;

import org.springframework.stereotype.Repository;

import com.codegnan.app.ecommercebackend.entity.Credential;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public class CredentialDaoImpl implements CredentialDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean saveCredential(Credential credential) {

        try {
            entityManager.persist(credential);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Credential getCredentialByUsername(String username) {

        String jpql =
                "SELECT c FROM Credential c " +
                "JOIN FETCH c.user u " +
                "WHERE LOWER(c.username) = LOWER(:username)";

        return entityManager
                .createQuery(jpql, Credential.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public Credential getCredentialByUserId(Long userId) {

        String jpql =
                "SELECT c FROM Credential c " +
                "WHERE c.user.userId = :userId";

        return entityManager
                .createQuery(jpql, Credential.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean updateCredential(Credential credential) {

        try {
            entityManager.merge(credential);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}