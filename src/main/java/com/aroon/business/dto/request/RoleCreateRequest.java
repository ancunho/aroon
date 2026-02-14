package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateRequest {

    @NotBlank(message = "{validation.role.code.required}")
    @Size(max = 50)
    private String roleCode;

    @NotBlank(message = "{validation.role.name.required}")
    @Size(max = 100)
    private String roleName;

    @Size(max = 255)
    private String description;

}
