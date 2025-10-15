package com.quizfun.questionbankquery.application.dto;

/**
 * Pagination metadata for query responses.
 */
public record PaginationMetadata(
        int currentPage,
        int pageSize,
        long totalItems,
        int totalPages
) {
    public static PaginationMetadata of(int currentPage, int pageSize, long totalItems) {
        int totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        return new PaginationMetadata(currentPage, pageSize, totalItems, totalPages);
    }
}
