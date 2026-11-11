package com.example.wonit.exception;

// 휴면 계좌에 입금하려 할 때 던지는 예외. 이름만 봐도 실패 이유를 알 수 있게 업무 이름으로 만든다.
public class AccountDormantException extends RuntimeException {
    public AccountDormantException(String message) {
        super(message);
    }
}
