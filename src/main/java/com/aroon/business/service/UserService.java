package com.aroon.business.service;

import com.aroon.business.entity.core.User;

import java.util.List;

public interface UserService {

    User createUser(User user);

    User getUserById(Long id);

    List<User> getAllUsers();

    User updateUser(User user);

    boolean deleteUser(Long id);
}
