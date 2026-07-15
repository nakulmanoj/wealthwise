package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.LoginRequest;
import com.nakul.wealthwise.dto.request.RegisterRequest;
import com.nakul.wealthwise.dto.response.AuthResponse;
import com.nakul.wealthwise.entity.User;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    User getProfile(String email);
}
