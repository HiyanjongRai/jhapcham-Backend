package com.example.jhapcham.common;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class PublicReferenceIdGenerator {

    public static final int DEFAULT_SUFFIX_LENGTH = 12;
    private static final int MIN_SUFFIX_LENGTH = 8;
    private static final int MAX_SUFFIX_LENGTH = 12;
    private static final int MAX_ATTEMPTS = 10;
    private static final String SAFE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kathmandu");
    private static final Pattern PUBLIC_REFERENCE_PATTERN = Pattern.compile(
            "^(PRD-RPT|SLR-RPT|CUS-RPT|REF)-\\d{8}-[A-HJ-NP-Z2-9]{8,12}$"
    );

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(PublicReferenceType type, Predicate<String> alreadyExists) {
        return generate(type, DEFAULT_SUFFIX_LENGTH, alreadyExists);
    }

    public String generate(PublicReferenceType type, int suffixLength, Predicate<String> alreadyExists) {
        if (type == null) {
            throw new IllegalArgumentException("Reference type is required");
        }
        if (suffixLength < MIN_SUFFIX_LENGTH || suffixLength > MAX_SUFFIX_LENGTH) {
            throw new IllegalArgumentException("Reference suffix length must be between 8 and 12 characters");
        }

        String datePart = LocalDate.now(BUSINESS_ZONE).format(DATE_FORMAT);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = type.getPrefix() + "-" + datePart + "-" + randomSuffix(suffixLength);
            if (alreadyExists == null || !alreadyExists.test(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique public reference ID after " + MAX_ATTEMPTS + " attempts");
    }

    public boolean isValid(String referenceId) {
        return referenceId != null && PUBLIC_REFERENCE_PATTERN.matcher(referenceId).matches();
    }

    public String requireValid(String referenceId) {
        String normalized = referenceId == null ? null : referenceId.trim().toUpperCase();
        if (!isValid(normalized)) {
            throw new IllegalArgumentException("Invalid public reference ID format");
        }
        return normalized;
    }

    private String randomSuffix(int length) {
        StringBuilder suffix = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            suffix.append(SAFE_ALPHABET.charAt(secureRandom.nextInt(SAFE_ALPHABET.length())));
        }
        return suffix.toString();
    }
}
