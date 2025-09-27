package com.quizfun.questionbank.domain.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class McqData {
    private final List<McqOption> options;
    private final boolean shuffleOptions;
    private final boolean allowMultipleCorrect;
    private final boolean allowPartialCredit;
    private final Integer timeLimitSeconds;

    public McqData(List<McqOption> options, boolean shuffleOptions,
                   boolean allowMultipleCorrect, boolean allowPartialCredit,
                   Integer timeLimitSeconds) {
        this.options = validateOptions(options);
        this.shuffleOptions = shuffleOptions;
        this.allowMultipleCorrect = allowMultipleCorrect;
        this.allowPartialCredit = allowPartialCredit;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    private List<McqOption> validateOptions(List<McqOption> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("MCQ must have at least one option");
        }

        boolean hasCorrectAnswer = options.stream().anyMatch(McqOption::isCorrect);
        if (!hasCorrectAnswer) {
            throw new IllegalArgumentException("MCQ must have at least one correct answer");
        }

        // Return defensive copy to ensure immutability
        return new ArrayList<>(options);
    }

    public List<McqOption> getOptions() {
        return new ArrayList<>(options); // Return defensive copy
    }

    public boolean isShuffleOptions() {
        return shuffleOptions;
    }

    public boolean isAllowMultipleCorrect() {
        return allowMultipleCorrect;
    }

    public boolean isAllowPartialCredit() {
        return allowPartialCredit;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public boolean hasCorrectAnswer() {
        return options.stream().anyMatch(McqOption::isCorrect);
    }

    public long getCorrectAnswerCount() {
        return options.stream().mapToLong(option -> option.isCorrect() ? 1 : 0).sum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        McqData mcqData = (McqData) o;
        return shuffleOptions == mcqData.shuffleOptions &&
               allowMultipleCorrect == mcqData.allowMultipleCorrect &&
               allowPartialCredit == mcqData.allowPartialCredit &&
               Objects.equals(options, mcqData.options) &&
               Objects.equals(timeLimitSeconds, mcqData.timeLimitSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(options, shuffleOptions, allowMultipleCorrect, allowPartialCredit, timeLimitSeconds);
    }

    @Override
    public String toString() {
        return String.format("McqData{optionCount=%d, shuffleOptions=%s, allowMultipleCorrect=%s, allowPartialCredit=%s, timeLimitSeconds=%s}",
                           options.size(), shuffleOptions, allowMultipleCorrect, allowPartialCredit, timeLimitSeconds);
    }
}