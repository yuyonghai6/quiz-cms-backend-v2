package com.quizfun.questionbankquery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application used to bootstrap the test context
 * for the question-bank-query module.
 *
 * <p>Component scanning includes:
 * <ul>
 *   <li>com.quizfun.questionbankquery - This module's components</li>
 *   <li>com.quizfun.globalshared - Mediator and shared utilities</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = {
        "com.quizfun.questionbankquery",
        "com.quizfun.globalshared"
})
public class TestQuestionBankQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestQuestionBankQueryApplication.class, args);
    }
}
