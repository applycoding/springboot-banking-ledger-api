package com.arpanbom.ledger.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
