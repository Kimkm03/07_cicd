package com.example.wonit.exception;

// 지급정지 계좌에서 출금하려 할 때 던지는 예외. 이름만 봐도 실패 이유를 알 수 있게 업무 이름으로 만든다.
public class AccountFrozenException extends RuntimeException {
    public AccountFrozenException(String message) {
        super(message);
    }
}