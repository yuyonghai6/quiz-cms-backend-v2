package com.quizfun.questionbank.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for enabling asynchronous method execution.
 *
 * This configuration enables Spring's @Async annotation support, which is used by:
 * - SecurityAuditLoggerImpl.logSecurityEventAsync() for non-blocking audit logging
 *
 * The async executor uses Spring's default SimpleAsyncTaskExecutor which creates
 * a new thread for each async task. For production use, consider configuring a
 * custom ThreadPoolTaskExecutor with appropriate pool sizes.
 *
 * @see com.quizfun.questionbank.application.security.SecurityAuditLoggerImpl
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
    // Uses Spring Boot's default async configuration
    // Can be customized by providing a TaskExecutor bean with name "taskExecutor"
}
