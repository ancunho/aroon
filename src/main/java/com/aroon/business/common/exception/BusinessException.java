package com.aroon.business.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode만으로 생성 (메시지는 GlobalExceptionHandler에서 MessageSource로 조회)
     * 예: throw new BusinessException(ErrorCode.USER_NOT_FOUND)
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode + 커스텀 메시지로 생성 (MessageSource 대신 직접 메시지 지정)
     * 예: throw new BusinessException(ErrorCode.BAD_REQUEST, "username은 3자 이상이어야 합니다")
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
