package com.aroon.business.common.exception;

import com.aroon.business.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * 1. 비즈니스 예외(우리가 직접 던지는 예외)
     * @param exception
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        Locale locale = LocaleContextHolder.getLocale();

        String message = messageSource.getMessage(
                errorCode.getMessageKey(),
                null,
                exception.getMessage(),
                locale
        );

        log.warn("BusinessException: code={}, message={}", errorCode, message);

        return ResponseEntity
                .status(resolveHttpStatus(errorCode))
                .body(ApiResponse.error(errorCode.getCode(), message));
    }

    /**
     * 2. 유효성 검증 예외 (@Valid 실패 시)
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("유효성 검증 실패");

        log.warn("ValidationException: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    /**
     * 3. 404 Not Found (존재하지 않는 URL 접근)
     * @param e
     * @return
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("not.found", null, "Resource not found", locale);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, message));
    }

    /**
     * 4. 기타 모든 예외 (예상치 못한 오류)
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("internal.error", null, "Internal server error", locale);

        log.error("Unhandled Exception: ", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), message));
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        return switch (errorCode.getCode()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> {
                // 인증 관련 에러 (5xxx)는 HTTP 401
                if (errorCode.getCode() >= 5000 && errorCode.getCode() < 6000) {
                    yield HttpStatus.UNAUTHORIZED;
                }
                // 1000번대 이상의 비즈니스 에러는 HTTP 200으로 응답
                // (HTTP 상태는 성공이지만, body의 code로 에러를 구분)
                if (errorCode.getCode() >= 1000) {
                    yield HttpStatus.OK;
                }
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
        };
    }
}
