package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoleResponse {

    private Long roleSeq;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
