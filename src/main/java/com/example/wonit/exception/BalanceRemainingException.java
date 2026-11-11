package com.example.wonit.exception;

// 해지. 🏦 금융 데이터는 지우지 않고 상태만 바꾼다 (Soft Delete). 거래 기록은 법적으로 수년간 보관해야 한다
public class BalanceRemainingException extends RuntimeException {
    public BalanceRemainingException(String message) {
        super(message);
    }
}
