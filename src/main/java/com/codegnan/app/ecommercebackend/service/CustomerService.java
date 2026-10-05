package com.codegnan.app.ecommercebackend.service;

import java.util.List;

import com.codegnan.app.ecommercebackend.dto.AddressRequestDto;
import com.codegnan.app.ecommercebackend.dto.AddressResponseDto;
import com.codegnan.app.ecommercebackend.dto.CustomerResponseDto;

public interface CustomerService {

    CustomerResponseDto getCustomer(Long userId);

    boolean updateCustomer(
            Long userId,
            CustomerResponseDto customerResponseDto
    );

    boolean addAddress(
            Long userId,
            AddressRequestDto addressRequestDto
    );

    List<AddressResponseDto> getAddresses(
            Long userId
    );

    boolean updateAddress(
            Long userId,
            Long addressId,
            AddressRequestDto addressRequestDto
    );

    boolean deactivateAddress(
            Long userId,
            Long addressId
    );
}