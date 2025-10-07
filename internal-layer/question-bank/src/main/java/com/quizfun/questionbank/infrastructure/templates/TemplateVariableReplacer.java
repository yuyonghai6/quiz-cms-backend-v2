package com.quizfun.questionbank.infrastructure.templates;

import com.quizfun.questionbank.domain.exceptions.TemplateProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for loading and processing JSON templates with variable replacement.
 */
@Component
public class TemplateVariableReplacer {

    private static final Logger logger = LoggerFactory.getLogger(TemplateVariableReplacer.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final String TEMPLATE_BASE_PATH = "templates/";

    /**
     * Replaces all template variables in the given template string.
     *
     * @param template The template string with {{VARIABLES}}
     * @param variables Map of variable names to values
     * @return Template with all variables replaced
     * @throws IllegalArgumentException if template or variables is null
     * @throws TemplateProcessingException if unreplaced variables remain
     */
    public String replace(String template, Map<String, String> variables) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        if (variables == null) {
            throw new IllegalArgumentException("Variables map cannot be null");
        }

        String result = template;

        // Replace all variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Check for unreplaced variables
        Matcher matcher = VARIABLE_PATTERN.matcher(result);
        if (matcher.find()) {
            String unreplacedVar = matcher.group(1);
            throw new TemplateProcessingException(
                "Unreplaced variable found: " + unreplacedVar
            );
        }

        return result;
    }

    /**
     * Loads template from classpath resources/templates/ directory.
     *
     * @param filename Template filename (e.g., "question_banks_per_user.json_template")
     * @return Template content as string
     * @throws TemplateProcessingException if template not found or cannot be read
     */
    public String loadTemplate(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE_PATH + filename);

            if (!resource.exists()) {
                throw new TemplateProcessingException("Template not found: " + filename);
            }

            String content = Files.readString(
                resource.getFile().toPath(),
                StandardCharsets.UTF_8
            );

            logger.debug("Successfully loaded template: {}", filename);
            return content;

        } catch (IOException ex) {
            logger.error("Failed to load template: {}", filename, ex);
            throw new TemplateProcessingException(
                "Failed to load template: " + filename,
                ex
            );
        }
    }
}
