package com.codegnan.app.ecommercebackend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.codegnan.app.ecommercebackend.dao.AddressDao;
import com.codegnan.app.ecommercebackend.dao.UserDao;
import com.codegnan.app.ecommercebackend.dto.AddressRequestDto;
import com.codegnan.app.ecommercebackend.dto.AddressResponseDto;
import com.codegnan.app.ecommercebackend.dto.CustomerResponseDto;
import com.codegnan.app.ecommercebackend.entity.Address;
import com.codegnan.app.ecommercebackend.entity.User;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

	private final UserDao userDao;
	private final AddressDao addressDao;

	public CustomerServiceImpl(UserDao userDao, AddressDao addressDao) {

		this.userDao = userDao;
		this.addressDao = addressDao;
	}

	@Override
	public CustomerResponseDto getCustomer(Long userId) {

		User user = userDao.getUserById(userId);

		if (user == null) {
			return null;
		}

		return convertUserToDto(user);
	}

	@Override
	public boolean updateCustomer(Long userId, CustomerResponseDto dto) {

		User user = userDao.getUserById(userId);

		if (user == null) {
			return false;
		}

		if (dto.getFirstName() != null) {

			user.setFirstName(dto.getFirstName().trim());
		}

		if (dto.getLastName() != null) {

			user.setLastName(dto.getLastName().trim());
		}

		if (dto.getPhone() != null) {

			user.setPhone(dto.getPhone().trim());
		}

		user.setUpdatedAt(LocalDateTime.now());

		return userDao.updateUser(user) != null;
	}

	@Override
	public boolean addAddress(Long userId, AddressRequestDto dto) {

		User user = userDao.getUserById(userId);

		if (user == null) {
			return false;
		}

		Address address = new Address();

		address.setUser(user);

		address.setLabel(dto.getLabel());

		address.setRecipientName(dto.getRecipientName());

		address.setPhone(dto.getPhone());

		address.setLine1(dto.getLine1());

		address.setLine2(dto.getLine2());

		address.setCity(dto.getCity());

		address.setState(dto.getState());

		address.setPostalCode(dto.getPostalCode());

		address.setCountryCode(dto.getCountryCode() == null ? "IN" : dto.getCountryCode().toUpperCase());

		address.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));

		address.setActive(true);

		LocalDateTime now = LocalDateTime.now();

		address.setCreatedAt(now);
		address.setUpdatedAt(now);

		return addressDao.saveAddress(address);
	}

	@Override
	public List<AddressResponseDto> getAddresses(Long userId) {

		return addressDao.getAddressesByUserId(userId).stream().map(this::convertAddressToDto)
				.collect(Collectors.toList());
	}

	@Override
	public boolean updateAddress(Long userId, Long addressId, AddressRequestDto dto) {

		Address address = addressDao.getAddressById(addressId);

		if (address == null || address.getUser() == null || !address.getUser().getUserId().equals(userId)) {

			return false;
		}

		if (dto.getLabel() != null) {
			address.setLabel(dto.getLabel());
		}

		if (dto.getRecipientName() != null) {
			address.setRecipientName(dto.getRecipientName());
		}

		if (dto.getPhone() != null) {
			address.setPhone(dto.getPhone());
		}

		if (dto.getLine1() != null) {
			address.setLine1(dto.getLine1());
		}

		if (dto.getLine2() != null) {
			address.setLine2(dto.getLine2());
		}

		if (dto.getCity() != null) {
			address.setCity(dto.getCity());
		}

		if (dto.getState() != null) {
			address.setState(dto.getState());
		}

		if (dto.getPostalCode() != null) {
			address.setPostalCode(dto.getPostalCode());
		}

		if (dto.getCountryCode() != null) {

			address.setCountryCode(dto.getCountryCode().toUpperCase());
		}

		if (dto.getIsDefault() != null) {

			address.setIsDefault(dto.getIsDefault());
		}

		address.setUpdatedAt(LocalDateTime.now());

		return addressDao.updateAddress(address);
	}

	@Override
	public boolean deactivateAddress(Long userId, Long addressId) {

		Address address = addressDao.getAddressById(addressId);

		if (address == null || address.getUser() == null || !address.getUser().getUserId().equals(userId)) {

			return false;
		}

		address.setActive(false);

		address.setIsDefault(false);

		address.setUpdatedAt(LocalDateTime.now());

		return addressDao.updateAddress(address);
	}

	private CustomerResponseDto convertUserToDto(User user) {

		CustomerResponseDto dto = new CustomerResponseDto();

		dto.setUserId(user.getUserId());

		dto.setFirstName(user.getFirstName());

		dto.setLastName(user.getLastName());

		dto.setEmail(user.getEmail());

		dto.setPhone(user.getPhone());

		dto.setActive(user.getActive());

		dto.setCreatedAt(user.getCreatedAt());

		dto.setUpdatedAt(user.getUpdatedAt());

		if (user.getRoles() != null) {

			dto.setRoles(user.getRoles().stream().map(role -> role.getRoleName()).collect(Collectors.toSet()));
		}

		return dto;
	}

	private AddressResponseDto convertAddressToDto(Address address) {

		AddressResponseDto dto = new AddressResponseDto();

		dto.setAddressId(address.getAddressId());

		dto.setLabel(address.getLabel());

		dto.setRecipientName(address.getRecipientName());

		dto.setPhone(address.getPhone());

		dto.setLine1(address.getLine1());

		dto.setLine2(address.getLine2());

		dto.setCity(address.getCity());

		dto.setState(address.getState());

		dto.setPostalCode(address.getPostalCode());

		dto.setCountryCode(address.getCountryCode());

		dto.setIsDefault(address.getIsDefault());

		dto.setActive(address.getActive());

		dto.setCreatedAt(address.getCreatedAt());

		dto.setUpdatedAt(address.getUpdatedAt());

		return dto;
	}
}