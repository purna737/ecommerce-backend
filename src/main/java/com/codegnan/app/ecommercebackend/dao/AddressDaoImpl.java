package com.codegnan.app.ecommercebackend.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.codegnan.app.ecommercebackend.entity.Address;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public class AddressDaoImpl implements AddressDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean saveAddress(Address address) {

        try {
            entityManager.persist(address);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Address> getAddressesByUserId(Long userId) {

        String jpql =
                "SELECT a FROM Address a " +
                "WHERE a.user.userId = :userId " +
                "ORDER BY a.isDefault DESC, a.addressId DESC";

        return entityManager
                .createQuery(jpql, Address.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public Address getAddressById(Long addressId) {

        return entityManager.find(Address.class, addressId);
    }

    @Override
    public boolean updateAddress(Address address) {

        try {
            entityManager.merge(address);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}