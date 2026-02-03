package com.zardx.tent.user.service;

public class TemporaryPasswordGenerator {
    public static String generate() {
        return new java.security.SecureRandom()
                .ints(6, 0, 62)
                .mapToObj("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"::charAt)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();

    }
}
