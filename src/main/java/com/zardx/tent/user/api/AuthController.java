package com.zardx.tent.user.api;

import com.zardx.tent.user.api.dto.EmailRequest;
import com.zardx.tent.user.api.dto.LoginRequest;
import com.zardx.tent.user.api.dto.RegisterRequest;
import com.zardx.tent.user.api.dto.VerifyOtpRequest;
import com.zardx.tent.user.model.User;
import com.zardx.tent.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * REGISTER user
     * Creates user (verified = false) and sends OTP
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody RegisterRequest request
    ) {
        authService.register(
                request.getEmail(),
                request.getName(),
                request.getPassword()
        );

        authService.sendOtp(request.getEmail());

        return ResponseEntity.status(201).build();
    }



    /**
     * VERIFY OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Void> verifyOtp(
            @RequestBody VerifyOtpRequest request
    ) {
        authService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * RESEND OTP
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(
            @RequestBody EmailRequest request
    ) {
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * LOGIN
     * Allowed only for verified users
     */
    @PostMapping("/login")
    public ResponseEntity<User> login(
            @RequestBody LoginRequest request
    ) {
        User user = authService.authenticate(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(user);
    }

    @PostMapping("/validateUser")
    public ResponseEntity<User> validateUser(
            @RequestBody EmailRequest request
    ) {
        User user = authService.validateUser(request.getEmail());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/getUserById")
    public ResponseEntity<User> getUserById(
            @RequestParam String userId
    ) {
        User user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/updateUser")
    public ResponseEntity<User> getUserById(
            @RequestParam String userId,
            @RequestBody User user
    ) {
        User updateUser = authService.updateUser(userId, user.getName());
        return ResponseEntity.ok(updateUser);
    }

    @PutMapping("/updatePassword")
    public ResponseEntity<User> updatePassword(
            @RequestParam String userId,
            @RequestBody LoginRequest request
    ) {
        User updateUser = authService.updatePassword(userId, request.getPassword());
        return ResponseEntity.ok(updateUser);
    }

    @PutMapping("/temporaryPassword")
    public ResponseEntity<User> tempPassword(
            @RequestParam String userId
    ) {
        User updateUser = authService.tempPassword(userId);
        return ResponseEntity.ok(updateUser);
    }


}
