package com.aroon.business.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @Size(max = 100)
    private String roleName;

    @Size(max = 255)
    private String description;

    private Integer status;

}
