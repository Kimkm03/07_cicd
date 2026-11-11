package com.example.wonit.dto;


public record AccountCreateRequest(
        Integer customerId,
        String accountType
) {
}