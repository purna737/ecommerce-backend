package com.codegnan.app.ecommercebackend.dao;

import java.util.List;

import com.codegnan.app.ecommercebackend.entity.Address;

public interface AddressDao {

    boolean saveAddress(Address address);

    List<Address> getAddressesByUserId(Long userId);

    Address getAddressById(Long addressId);

    boolean updateAddress(Address address);
}