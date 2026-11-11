package com.example.wonit.dto;

import jakarta.validation.constraints.Pattern;

public record AccountPatchRequest(
        String accountType,
        String status
) {}