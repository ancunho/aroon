package com.aroon.business.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    /**
     * 데이터가 있는 성공 응답
     * 예: ApiResponse.success(userResponse)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 메시지를 지정한 성공 응답 (다국어 메시지 사용 시)
     * 예: ApiResponse.success(userResponse, "성공")
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

    }

    /**
     * 데이터 없는 성공 응답 (삭제 등)
     * 예: ApiResponse.success()
     */
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Success")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 에러 응답
     * 예: ApiResponse.error(404, "사용자를 찾을 수 없습니다")
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

}
