package com.smartquiz.service;

import com.smartquiz.entity.User;

public interface JwtService {

    String generateToken(User user);

    String extractEmail(String token);

    Long extractUserId(String token);

    boolean isTokenValid(String token);
}
