package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.request.UserCreateRequest;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.entity.core.User;
import com.aroon.business.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @GetMapping("/{userSeq}")
    public ApiResponse<UserResponse> getUserBySeq(@PathVariable Long userSeq) {
        return ApiResponse.success(userService.getUserBySeq(userSeq));
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody UserCreateRequest userCreateRequest) {
        return ApiResponse.success(userService.createUser(userCreateRequest));
    }

}
