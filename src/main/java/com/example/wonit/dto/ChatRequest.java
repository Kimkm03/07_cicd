package com.example.wonit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 챗봇에게 보내는 질문 한 줄. 너무 긴 글은 받지 않는다 (AI 요금은 글자 수만큼 나온다)
public record ChatRequest(@NotBlank @Size(max = 500) String message) {}
