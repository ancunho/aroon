package com.aroon.business.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    private ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.data = null;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 데이터가 있는 성공 응답
     * 예: ApiResponse.success(userResponse)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    /**
     * 메시지를 지정한 성공 응답 (다국어 메시지 사용 시)
     * 예: ApiResponse.success(userResponse, "성공")
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 데이터 없는 성공 응답 (삭제 등)
     * 예: ApiResponse.success()
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "Success");
    }

    /**
     * 에러 응답
     * 예: ApiResponse.error(404, "사용자를 찾을 수 없습니다")
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message);
    }

}
