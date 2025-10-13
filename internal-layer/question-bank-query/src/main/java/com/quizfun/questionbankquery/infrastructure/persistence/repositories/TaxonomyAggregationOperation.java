package com.quizfun.questionbankquery.infrastructure.persistence.repositories;

import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;

import java.util.Arrays;

/**
 * Custom MongoDB aggregation operation to build taxonomy object from relationships.
 *
 * Uses raw BSON Documents instead of SpEL to avoid parsing errors.
 * This stage transforms the joined taxonomy_relationships array into a structured taxonomy object.
 */
public class TaxonomyAggregationOperation implements AggregationOperation {

    @Override
    public Document toDocument(AggregationOperationContext context) {
        // Build the $addFields stage using raw BSON Document
        return new Document("$addFields", new Document("taxonomy",
            new Document()
                // Extract categories from relationships with taxonomy_type matching "category_level_*"
                .append("categories", new Document("$map", new Document()
                    .append("input", new Document("$filter", new Document()
                        .append("input", "$taxonomy_relationships")
                        .append("as", "rel")
                        .append("cond", new Document("$regexMatch", new Document()
                            .append("input", "$$rel.taxonomy_type")
                            .append("regex", "^category_level_")
                        ))
                    ))
                    .append("as", "cat")
                    .append("in", "$$cat.taxonomy_id")
                ))

                // Extract tags from relationships with taxonomy_type = "tag"
                .append("tags", new Document("$map", new Document()
                    .append("input", new Document("$filter", new Document()
                        .append("input", "$taxonomy_relationships")
                        .append("as", "rel")
                        .append("cond", new Document("$eq", Arrays.asList("$$rel.taxonomy_type", "tag")))
                    ))
                    .append("as", "tag")
                    .append("in", "$$tag.taxonomy_id")
                ))

                // Extract quizzes from relationships with taxonomy_type = "quiz"
                .append("quizzes", new Document("$map", new Document()
                    .append("input", new Document("$filter", new Document()
                        .append("input", "$taxonomy_relationships")
                        .append("as", "rel")
                        .append("cond", new Document("$eq", Arrays.asList("$$rel.taxonomy_type", "quiz")))
                    ))
                    .append("as", "quiz")
                    .append("in", "$$quiz.taxonomy_id")
                ))

                // Extract difficulty level (first match with taxonomy_type = "difficulty_level")
                .append("difficultyLevel", new Document("$first", new Document("$map", new Document()
                    .append("input", new Document("$filter", new Document()
                        .append("input", "$taxonomy_relationships")
                        .append("as", "rel")
                        .append("cond", new Document("$eq", Arrays.asList("$$rel.taxonomy_type", "difficulty_level")))
                    ))
                    .append("as", "diff")
                    .append("in", "$$diff.taxonomy_id")
                )))
        ));
    }
}
