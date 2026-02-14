package com.aroon.business.controller;

import com.aroon.business.common.response.ApiResponse;
import com.aroon.business.dto.response.UserResponse;
import com.aroon.business.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
