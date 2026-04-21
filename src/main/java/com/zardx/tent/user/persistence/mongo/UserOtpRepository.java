package com.zardx.tent.user.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserOtpRepository
        extends MongoRepository<UserOtpDocument, String> {

    Optional<UserOtpDocument> findByEmail(String email);

    void deleteByEmail(String email);
}
