package com.zardx.tent.user.persistence.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "user_otps")
@Getter
@Setter
@CompoundIndex(
        name = "email_unique_idx",
        def = "{'email': 1}",
        unique = true
)
public class UserOtpDocument {

    @Id
    private String id;

    private String email;

    /** BCrypt hash of OTP */
    private String otpHash;

    private Instant expiresAt;

    private Instant createdAt;
}

