package com.alkacode.vips.util;

import java.security.SecureRandom;

public final class KeyGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private KeyGenerator() {
    }

    public static String generate() {
        StringBuilder builder = new StringBuilder("ALK-");
        for (int i = 0; i < 6; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
