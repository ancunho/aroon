package com.aroon.business.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long userSeq;
    private String userId;
    private String email;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
