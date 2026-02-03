package com.zardx.tent.user.service;

public interface EmailService {
    void sendOtp(String email, String otp);
    void sendTempPassword(String email, String password);
}
