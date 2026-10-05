package com.codegnan.app.ecommercebackend.resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codegnan.app.ecommercebackend.dto.LoginResponseDto;
import com.codegnan.app.ecommercebackend.dto.SignInRequestDto;
import com.codegnan.app.ecommercebackend.dto.SignUpRequestDto;
import com.codegnan.app.ecommercebackend.service.AuthenticationService;

@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5500",
		"http://127.0.0.1:5500" })
@RestController
@RequestMapping("/rest/api/v1/auth")
public class AuthenticationResource {

	private final AuthenticationService authenticationService;

	public AuthenticationResource(AuthenticationService authenticationService) {

		this.authenticationService = authenticationService;
	}

	@PostMapping("/register")
	public ResponseEntity<String> register(@RequestBody SignUpRequestDto signUpRequestDto) {

		boolean result = authenticationService.registerCustomer(signUpRequestDto);

		if (result) {

			return ResponseEntity.status(HttpStatus.CREATED).body("Customer registration successful");
		}

		return ResponseEntity.badRequest().body("Customer registration failed");
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponseDto> login(@RequestBody SignInRequestDto signInRequestDto) {

		LoginResponseDto response = authenticationService.login(signInRequestDto);

		if (response == null) {

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		return ResponseEntity.ok(response);
	}
}