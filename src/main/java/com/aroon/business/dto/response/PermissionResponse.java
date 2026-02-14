package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PermissionResponse {

    private Long permissionSeq;
    private String permissionCode;
    private String permissionName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
