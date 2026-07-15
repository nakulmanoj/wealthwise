package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.ChangePasswordRequest;
import com.nakul.wealthwise.dto.request.UpdateProfileRequest;
import com.nakul.wealthwise.entity.User;

public interface UserService {
    User getProfile(String email);
    User updateProfile(String email, UpdateProfileRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    void deactivateAccount(String email);
}
