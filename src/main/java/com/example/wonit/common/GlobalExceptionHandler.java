package com.example.wonit.common;

import com.example.wonit.dto.ErrorResponse;
import com.example.wonit.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 손님 잘못 → WARN 한 줄. 스택트레이스까지 찍으면 로그가 쓰레기로 가득 찬다
    @ExceptionHandler(AccountFrozenException.class)
    public ResponseEntity<ErrorResponse> handleAccountFrozen(AccountFrozenException e) {
        log.warn("지급정지 계좌 출금 시도: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ACCOUNT_FROZEN", e.getMessage()));
    }

    @ExceptionHandler(AccountDormantException.class)
    public ResponseEntity<ErrorResponse> handleAccountDormant(AccountDormantException e) {
        log.warn("휴면 계좌 입금 시도: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ACCOUNT_DORMANT", e.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException e) {
        log.warn("잔액 부족: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INSUFFICIENT_BALANCE", e.getMessage()));
    }

    @ExceptionHandler(InvalidAccountRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAccountRequest(
            InvalidAccountRequestException e
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_ACCOUNT_REQUEST", e.getMessage()));
    }

    // 없는 계좌 조회 예외 추가
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException e) {
        log.warn("계좌 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ACCOUNT_NOT_FOUND", e.getMessage()));
    }

    // 맨 마지막 그물. 🚨 e.getMessage() 를 그대로 내보내면 SQL·클래스명이 손님 화면에 찍힌다s
//    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
//        log.error("처리하지 못한 예외", e);   // ← e 를 {} 없이 마지막에 넘기면 스택트레이스 전체가 찍힌다
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(new ErrorResponse("INTERNAL_ERROR", "잠시 후 다시 시도해 주세요."));
//    }

    // 맨 마지막 그물. 🚨 e.getMessage() 를 그대로 내보내면 SQL·클래스명이 손님 화면에 찍힌다
    // @Valid 검사에 걸리면 → 400. DTO 에 적어 둔 message(예: "입금 금액은 0보다 커야 합니다")를 그대로 돌려준다
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getDefaultMessage()).orElse("요청 값이 올바르지 않습니다.");
        log.warn("요청 값 검사 실패: {}", message);
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        // ① 스프링이 이미 상태코드를 정해 둔 예외 → 그 코드 그대로 (없는 주소 404, 허용 안 된 메서드 405, 필수값 누락 400, ResponseStatusException 등)
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            log.warn("요청 오류 {}: {}", status.value(), e.getMessage());
            return ResponseEntity.status(status)
                    .body(new ErrorResponse(status.name(), springError.getBody().getDetail()));
        }
        // ② 깨진 JSON, 숫자 자리에 글자 → 손님 잘못이므로 400
        if (e instanceof HttpMessageNotReadableException || e instanceof MethodArgumentTypeMismatchException) {
            log.warn("요청 형식 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("BAD_REQUEST", "요청 형식이 올바르지 않습니다."));
        }
        // ③ 나머지 = 진짜 서버 잘못 → 500
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "잠시 후 다시 시도해 주세요."));
    }
}