package com.example.auto4jobs.validation;

import java.util.regex.Pattern;

public class EmailValidator {

    // Regex for email validation
    private static final String EMAIL_REGEX = 
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9._-]+\\.[A-Z|a-z]{2,}$";

    // Maximum allowed email length as per RFC 5321
    private static final int MAX_EMAIL_LENGTH = 254;

    // Minimum allowed email length
    private static final int MIN_EMAIL_LENGTH = 6;

    // Blocked email domains
    private static final String[] BLOCKED_DOMAINS = {
        "mailinator.com", "temp-mail.org", "guerrillamail.com", 
        "throwawaymail.com", "10minutemail.com"
    };

    public static boolean isValidEmail(String email) {
        // Null or empty check
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Length validation
        if (email.length() > MAX_EMAIL_LENGTH || email.length() < MIN_EMAIL_LENGTH) {
            return false;
        }

        // Regex validation with case-insensitive matching
        boolean isValidFormat = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE)
            .matcher(email)
            .matches();

        // Additional lightweight checks
        if (isValidFormat) {
            // Prevent consecutive dots in local part
            if (email.contains("..")) {
                return false;
            }

            // Ensure domain has at least one dot
            String[] parts = email.split("@");
            if (parts.length == 2) {
                // Check for blocked domains
                for (String blockedDomain : BLOCKED_DOMAINS) {
                    if (parts[1].toLowerCase().equals(blockedDomain)) {
                        return false;
                    }
                }

                // Ensure domain has at least one dot
                if (!parts[1].contains(".")) {
                    return false;
                }
            }
        }

        return isValidFormat;
    }

    // Method to extract email domain
    public static String getEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        return email.split("@")[1];
    }
} 