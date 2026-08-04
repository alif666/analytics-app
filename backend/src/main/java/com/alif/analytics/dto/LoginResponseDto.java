package com.alif.analytics.dto;

public record LoginResponseDto(String message, UserDto user, String jwtToken) {
}

