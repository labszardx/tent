package com.zardx.tent.user.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super(email + " is already registered");
    }
}
