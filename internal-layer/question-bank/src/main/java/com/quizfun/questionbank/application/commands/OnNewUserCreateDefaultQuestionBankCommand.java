package com.quizfun.questionbank.application.commands;

import com.quizfun.globalshared.mediator.ICommand;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Command to create a default question bank for a new user.
 * <p>
 * This command is immutable and validates all inputs during construction.
 * It implements ICommand to work with the CQRS mediator pattern.
 * </p>
 */
public class OnNewUserCreateDefaultQuestionBankCommand
        implements ICommand<DefaultQuestionBankResponseDto> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final Long userId;
    private final String userEmail;
    private final Map<String, String> metadata;

    /**
     * Constructs a new command with validation.
     *
     * @param userId The unique identifier for the user (must be positive)
     * @param userEmail The user's email address (optional, but must be valid if provided)
     * @param metadata Additional metadata (optional, will be made immutable)
     * @throws IllegalArgumentException if userId is null or non-positive, or email format is invalid
     */
    public OnNewUserCreateDefaultQuestionBankCommand(
            Long userId,
            String userEmail,
            Map<String, String> metadata) {

        // Validate userId
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }

        this.userId = userId;
        this.userEmail = validateAndTrimEmail(userEmail);
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    /**
     * Validates and trims the email address.
     *
     * @param email The email to validate
     * @return Trimmed email or null if empty/null
     * @throws IllegalArgumentException if email format is invalid
     */
    private String validateAndTrimEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + trimmed);
        }

        return trimmed;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
