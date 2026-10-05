package com.codegnan.app.ecommercebackend.dao;

import com.codegnan.app.ecommercebackend.entity.User;

public interface UserDao {

    boolean saveUser(User user);

    User getUserById(Long userId);

    User getUserByEmail(String email);

    User updateUser(User user);
}