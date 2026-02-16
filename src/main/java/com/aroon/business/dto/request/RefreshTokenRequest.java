package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token을 입력해주세요")
    private String refreshToken;
}
