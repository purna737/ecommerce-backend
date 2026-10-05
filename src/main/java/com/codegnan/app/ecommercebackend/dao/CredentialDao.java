package com.codegnan.app.ecommercebackend.dao;

import com.codegnan.app.ecommercebackend.entity.Credential;

public interface CredentialDao {

    boolean saveCredential(Credential credential);

    Credential getCredentialByUsername(String username);

    Credential getCredentialByUserId(Long userId);

    boolean updateCredential(Credential credential);
}