package com.zardx.tent.user.service;

import com.zardx.tent.user.model.User;

public interface AuthService {

    User register(String email, String name, String password);

    void sendOtp(String email);

    void verifyOtp(String email, String otp);

    User authenticate(String email, String password);

    User validateUser(String email);

    User getUserById(String userId);

    User updateUser(String userId, String name);

    User updatePassword(String userId, String password);

    User tempPassword(String userId);
}

