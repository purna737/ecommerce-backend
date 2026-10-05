package com.codegnan.app.ecommercebackend.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codegnan.app.ecommercebackend.dto.AddressRequestDto;
import com.codegnan.app.ecommercebackend.dto.AddressResponseDto;
import com.codegnan.app.ecommercebackend.dto.CustomerResponseDto;
import com.codegnan.app.ecommercebackend.service.CustomerService;

@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5500",
		"http://127.0.0.1:5500" })
@RestController
@RequestMapping("/rest/api/v1/me")
public class CustomerResource {

	private final CustomerService customerService;

	public CustomerResource(CustomerService customerService) {

		this.customerService = customerService;
	}

	@GetMapping
	public ResponseEntity<CustomerResponseDto> getCustomer(@RequestParam Long userId) {

		CustomerResponseDto response = customerService.getCustomer(userId);

		if (response == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(response);
	}

	@PutMapping
	public ResponseEntity<String> updateCustomer(@RequestParam Long userId,
			@RequestBody CustomerResponseDto customerResponseDto) {

		boolean result = customerService.updateCustomer(userId, customerResponseDto);

		if (!result) {

			return ResponseEntity.badRequest().body("Customer update failed");
		}

		return ResponseEntity.ok("Customer updated successfully");
	}

	@GetMapping("/addresses")
	public ResponseEntity<List<AddressResponseDto>> getAddresses(@RequestParam Long userId) {

		return ResponseEntity.ok(customerService.getAddresses(userId));
	}

	@PostMapping("/addresses")
	public ResponseEntity<String> addAddress(@RequestParam Long userId,
			@RequestBody AddressRequestDto addressRequestDto) {

		boolean result = customerService.addAddress(userId, addressRequestDto);

		if (!result) {

			return ResponseEntity.badRequest().body("Address creation failed");
		}

		return ResponseEntity.ok("Address added successfully");
	}

	@PutMapping("/addresses/{addressId}")
	public ResponseEntity<String> updateAddress(@RequestParam Long userId, @PathVariable Long addressId,
			@RequestBody AddressRequestDto addressRequestDto) {

		boolean result = customerService.updateAddress(userId, addressId, addressRequestDto);

		if (!result) {

			return ResponseEntity.badRequest().body("Address update failed");
		}

		return ResponseEntity.ok("Address updated successfully");
	}

	@DeleteMapping("/addresses/{addressId}")
	public ResponseEntity<String> deactivateAddress(@RequestParam Long userId, @PathVariable Long addressId) {

		boolean result = customerService.deactivateAddress(userId, addressId);

		if (!result) {

			return ResponseEntity.badRequest().body("Address deactivation failed");
		}

		return ResponseEntity.ok("Address deactivated successfully");
	}
}