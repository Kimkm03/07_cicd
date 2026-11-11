package com.example.wonit.dto;

import java.math.BigDecimal;

// 카테고리별 소비(출금) 합계 한 줄. DB 의 group by 결과를 그대로 담는 상자
public record CategorySpending(String category, BigDecimal total, Long count) {}
