package com.example.wonit.exception;

// 계좌가 없을 때 던지는 예외. 이름만 봐도 실패 이유를 알 수 있게 업무 이름으로 만든다.
// RuntimeException 을 상속하면 메서드마다 throws 를 적지 않아도 된다.
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
