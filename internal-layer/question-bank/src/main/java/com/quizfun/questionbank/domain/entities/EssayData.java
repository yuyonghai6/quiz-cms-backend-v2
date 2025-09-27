package com.quizfun.questionbank.domain.entities;

import java.util.Objects;

public class EssayData {
    private final Integer minWordCount;
    private final Integer maxWordCount;
    private final boolean allowRichText;
    private final EssayRubric rubric;

    public EssayData(Integer minWordCount, Integer maxWordCount,
                     boolean allowRichText, EssayRubric rubric) {
        validateWordCounts(minWordCount, maxWordCount);

        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.allowRichText = allowRichText;
        this.rubric = rubric; // Can be null
    }

    private void validateWordCounts(Integer minWordCount, Integer maxWordCount) {
        if (minWordCount != null && minWordCount < 0) {
            throw new IllegalArgumentException("minimum word count cannot be negative");
        }
        if (maxWordCount != null && maxWordCount < 0) {
            throw new IllegalArgumentException("maximum word count cannot be negative");
        }
        if (minWordCount != null && maxWordCount != null && minWordCount > maxWordCount) {
            throw new IllegalArgumentException("minimum word count cannot exceed maximum word count");
        }
    }

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public boolean isAllowRichText() {
        return allowRichText;
    }

    public EssayRubric getRubric() {
        return rubric;
    }

    public boolean hasWordCountLimits() {
        return minWordCount != null || maxWordCount != null;
    }

    public boolean hasRubric() {
        return rubric != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EssayData essayData = (EssayData) o;
        return allowRichText == essayData.allowRichText &&
               Objects.equals(minWordCount, essayData.minWordCount) &&
               Objects.equals(maxWordCount, essayData.maxWordCount) &&
               Objects.equals(rubric, essayData.rubric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minWordCount, maxWordCount, allowRichText, rubric);
    }

    @Override
    public String toString() {
        return String.format("EssayData{minWordCount=%s, maxWordCount=%s, allowRichText=%s, hasRubric=%s}",
                           minWordCount, maxWordCount, allowRichText, hasRubric());
    }
}