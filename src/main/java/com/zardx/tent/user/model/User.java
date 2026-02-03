package com.zardx.tent.user.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class User {

    private String id;
    private String email;
    private String name;
    private boolean verified;
    private Instant createdAt;
    private Instant updatedAt;
}
