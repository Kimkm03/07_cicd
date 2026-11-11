package com.example.wonit.dto;

// 클래스 대신 final로 값을 전달만 하는 data를 위한 빠르게 만드는 클래스
// { code : "404", message : "9990 계좌를 찾을 수 없습니다."}
public record ErrorResponse (String code, String message) {
}
