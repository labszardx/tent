package com.zardx.tent.user.persistence.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Getter
@Setter
public class UserDocument {

    @Id
    private String id;

    private String email;
    private String name;

    private String passwordHash;

    private boolean verified;

    private Instant createdAt;
    private Instant updatedAt;
}
