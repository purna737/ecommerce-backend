package com.codegnan.app.ecommercebackend.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.codegnan.app.ecommercebackend.dao.CredentialDao;
import com.codegnan.app.ecommercebackend.dao.UserDao;
import com.codegnan.app.ecommercebackend.dto.LoginResponseDto;
import com.codegnan.app.ecommercebackend.dto.SignInRequestDto;
import com.codegnan.app.ecommercebackend.dto.SignUpRequestDto;
import com.codegnan.app.ecommercebackend.entity.Credential;
import com.codegnan.app.ecommercebackend.entity.Role;
import com.codegnan.app.ecommercebackend.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

	private final UserDao userDao;
	private final CredentialDao credentialDao;

	@PersistenceContext
	private EntityManager entityManager;

	public AuthenticationServiceImpl(UserDao userDao, CredentialDao credentialDao) {
		this.userDao = userDao;
		this.credentialDao = credentialDao;
	}

	@Override
	public boolean registerCustomer(SignUpRequestDto dto) {

		if (dto == null) {
			return false;
		}

		if (dto.getEmail() == null || dto.getUsername() == null || dto.getPassword() == null) {

			return false;
		}

		// Check whether email already exists
		User existingUser = userDao.getUserByEmail(dto.getEmail().trim());

		if (existingUser != null) {
			return false;
		}

		// Check whether username already exists
		Credential existingCredential = credentialDao.getCredentialByUsername(dto.getUsername().trim());

		if (existingCredential != null) {
			return false;
		}

		LocalDateTime now = LocalDateTime.now();

		// Create User
		User user = new User();

		user.setFirstName(dto.getFirstName().trim());
		user.setLastName(dto.getLastName().trim());
		user.setEmail(dto.getEmail().trim().toLowerCase());
		user.setPhone(dto.getPhone());
		user.setActive(true);
		user.setCreatedAt(now);
		user.setUpdatedAt(now);

		// Find CUSTOMER role
		Role customerRole = entityManager.createQuery("SELECT r FROM Role r WHERE r.roleName = :roleName", Role.class)
				.setParameter("roleName", "CUSTOMER").getSingleResult();

		// Assign CUSTOMER role
		Set<Role> roles = new HashSet<>();

		roles.add(customerRole);

		user.setRoles(roles);

		// Save User
		userDao.saveUser(user);

		// Create Credential
		Credential credential = new Credential();

		credential.setUser(user);

		credential.setUsername(dto.getUsername().trim().toLowerCase());

		// Store password
		credential.setPasswordHash(dto.getPassword());

		credential.setEnabled(true);

		credential.setFailedLoginAttempts(0);

		credential.setLockedUntil(null);

		credential.setPasswordChangedAt(now);

		credential.setLastLoginAt(null);

		credential.setCreatedAt(now);

		credential.setUpdatedAt(now);

		// Save Credential
		return credentialDao.saveCredential(credential);
	}

	@Override
	public LoginResponseDto login(SignInRequestDto dto) {

		if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {

			return null;
		}

		// Find credential using username
		Credential credential = credentialDao.getCredentialByUsername(dto.getUsername().trim());

		if (credential == null) {
			return null;
		}

		// Get User
		User user = credential.getUser();

		if (user == null || !Boolean.TRUE.equals(user.getActive())) {

			return null;
		}

		// Check credential status
		if (!Boolean.TRUE.equals(credential.getEnabled())) {
			return null;
		}

		LocalDateTime now = LocalDateTime.now();

		// Check account lock
		if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(now)) {

			return null;
		}

		// Compare password
		boolean passwordMatches = dto.getPassword().equals(credential.getPasswordHash());

		// Wrong password
		if (!passwordMatches) {

			int attempts = credential.getFailedLoginAttempts() == null ? 0 : credential.getFailedLoginAttempts();

			credential.setFailedLoginAttempts(attempts + 1);

			credential.setUpdatedAt(now);

			credentialDao.updateCredential(credential);

			return null;
		}

		// Successful login
		credential.setFailedLoginAttempts(0);

		credential.setLockedUntil(null);

		credential.setLastLoginAt(now);

		credential.setUpdatedAt(now);

		credentialDao.updateCredential(credential);

		// Get user roles
		Set<String> roles = new HashSet<>();

		if (user.getRoles() != null) {

			user.getRoles().forEach(role -> roles.add(role.getRoleName()));
		}

		// Return login response
		return new LoginResponseDto("Login successful", user.getUserId(), credential.getUsername(), user.getFirstName(),
				roles);
	}

	// Check whether the user is an ADMIN
	@Override
	public boolean isAdmin(Long userId) {

		User user = userDao.getUserById(userId);

		if (user == null) {
			return false;
		}

		if (!Boolean.TRUE.equals(user.getActive())) {
			return false;
		}

		if (user.getRoles() == null) {
			return false;
		}

		return user.getRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getRoleName()));
	}
}