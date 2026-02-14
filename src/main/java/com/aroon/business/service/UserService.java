package com.aroon.business.service;

import com.aroon.business.dto.request.UserCreateRequest;
import com.aroon.business.dto.request.UserUpdateRequest;
import com.aroon.business.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUserBySeq(Long userSeq);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long userSeq, UserUpdateRequest request);

    void deleteUser(Long userSeq);

}
