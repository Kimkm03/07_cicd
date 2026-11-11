package com.example.wonit.dto;

import java.math.BigDecimal;

// 한 달치 입금 · 출금 합계. month 는 "2026-09" 형식
public record MonthlySummary(String month, BigDecimal income, BigDecimal spending) {}
