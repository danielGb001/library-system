package com.library.librarysystem.controller;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
