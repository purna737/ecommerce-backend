package com.codegnan.app.ecommercebackend.service;

import com.codegnan.app.ecommercebackend.dto.LoginResponseDto;
import com.codegnan.app.ecommercebackend.dto.SignInRequestDto;
import com.codegnan.app.ecommercebackend.dto.SignUpRequestDto;

public interface AuthenticationService {

    boolean registerCustomer(SignUpRequestDto signUpRequestDto);

    LoginResponseDto login(SignInRequestDto signInRequestDto);

    boolean isAdmin(Long userId);
}