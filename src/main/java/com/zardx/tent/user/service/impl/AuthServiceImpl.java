package com.zardx.tent.user.service.impl;

import com.zardx.tent.user.exception.*;
import com.zardx.tent.user.model.User;
import com.zardx.tent.user.persistence.mongo.UserDocument;
import com.zardx.tent.user.persistence.mongo.UserOtpDocument;
import com.zardx.tent.user.persistence.mongo.UserOtpRepository;
import com.zardx.tent.user.persistence.mongo.UserRepository;
import com.zardx.tent.user.service.AuthService;
import com.zardx.tent.user.service.EmailService;
import com.zardx.tent.user.service.OtpGenerator;
import com.zardx.tent.user.service.TemporaryPasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /* =========================
       REGISTER
       ========================= */

    @Override
    public User register(String email, String name, String rawPassword) {

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        UserDocument user = new UserDocument();
        user.setId(email);
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setVerified(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        UserDocument saved = userRepository.save(user);

        return toDomain(saved);
    }

    /* =========================
       SEND / RESEND OTP
       ========================= */

    @Override
    public void sendOtp(String email) {

        UserDocument user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (user.isVerified()) {
            return; // already verified, no OTP needed
        }

        // Generate OTP
        String otp = OtpGenerator.generate();

        // Hash OTP
        String otpHash = passwordEncoder.encode(otp);

        // Upsert OTP (one active OTP per email)
        UserOtpDocument otpDoc = otpRepository.findByEmail(email)
                .orElseGet(UserOtpDocument::new);

        otpDoc.setEmail(email);
        otpDoc.setOtpHash(otpHash);
        otpDoc.setCreatedAt(Instant.now());
        otpDoc.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));

        otpRepository.save(otpDoc);

        // Send email
        emailService.sendOtp(email, otp);
    }

    /* =========================
       VERIFY OTP
       ========================= */

    @Override
    public void verifyOtp(String email, String otp) {

        UserOtpDocument otpDoc = otpRepository.findByEmail(email)
                .orElseThrow(InvalidOtpException::new);

        if (Instant.now().isAfter(otpDoc.getExpiresAt())) {
            throw new OtpExpiredException();
        }

        if (!passwordEncoder.matches(otp, otpDoc.getOtpHash())) {
            throw new InvalidOtpException();
        }

        UserDocument user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        user.setVerified(true);
        userRepository.save(user);

        otpRepository.deleteByEmail(email);
    }

    /* =========================
       LOGIN
       ========================= */

    @Override
    public User authenticate(String email, String rawPassword) {

        UserDocument user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isVerified()) {
            throw new UserNotVerifiedException();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return toDomain(user);
    }

    @Override
    public User validateUser(String email) {
        UserDocument user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isVerified()) {
            throw new UserNotVerifiedException();
        }

        return toDomain(user);
    }

    @Override
    public User getUserById(String userId) {
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isVerified()) {
            throw new UserNotVerifiedException();
        }

        return toDomain(user);
    }

    @Override
    public User updateUser(String userId, String name) {
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isVerified()) {
            throw new UserNotVerifiedException();
        }
        user.setName(name);
        user.setUpdatedAt(Instant.now());
        return toDomain(userRepository.save(user));
    }

    @Override
    public User updatePassword(String userId, String rawPassword) {
        UserDocument user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isVerified()) {
            throw new UserNotVerifiedException();
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setUpdatedAt(Instant.now());
        return toDomain(userRepository.save(user));
    }

    @Override
    public User tempPassword(String userId) {
        String generated = TemporaryPasswordGenerator.generate();
        User user = updatePassword(userId, generated);
        //temp pass sent
        emailService.sendTempPassword(user.getEmail(), generated);
        return user;
    }

    /* =========================
       MAPPING
       ========================= */

    private User toDomain(UserDocument doc) {
        User user = new User();
        user.setId(doc.getId());
        user.setEmail(doc.getEmail());
        user.setName(doc.getName());
        user.setVerified(doc.isVerified());
        user.setCreatedAt(doc.getCreatedAt());
        user.setUpdatedAt(doc.getUpdatedAt());
        return user;
    }
}
