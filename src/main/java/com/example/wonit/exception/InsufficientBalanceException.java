package com.example.wonit.exception;

// 잔액이 모자랄 때 던지는 예외. 이름만 봐도 실패 이유를 알 수 있게 업무 이름으로 만든다.
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}