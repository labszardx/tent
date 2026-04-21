package com.zardx.tent.user.service.impl;

import com.zardx.tent.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendOtp(String email, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Verify your email - tent");
        message.setText(buildOtpBody(otp));

        mailSender.send(message);
    }

    @Override
    public void sendTempPassword(String email, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Temporary password for your account - tent");
        message.setText(buildTempPasswordBody(password));

        mailSender.send(message);
    }

    private String buildOtpBody(String otp) {
        return "Your tent verification code is:\n\n"
                + otp
                + "\n\nThis code is valid for 5 minutes.\n\n"
                + "If you did not request this, please ignore this email.";
    }

    private String buildTempPasswordBody(String password) {
        return "Your tent account temporary password is:\n\n"
                + password + "\n\n"
                + "For security reasons, please sign in using this temporary password and "
                + "update your password from the Profile section after logging in.\n\n"
                + "If you did not request this email, please disregard it.";

    }
}
