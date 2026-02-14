package com.aroon.business.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(min = 8, max = 100, message = "{validation.password.size}")
    private String password;

    @Email(message = "{validation.email.format}")
    private String email;

    private String nickname;

    private Integer status;

    private String mobile;

}
