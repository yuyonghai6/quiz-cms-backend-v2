package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.*;
import com.quizfun.globalshared.utils.UUIDv7Generator;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class QuestionAggregateBehaviorMethodsTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateBehaviorMethodsTest.Should publish question with valid type-specific data")
    @Description("publish() should set status to published, set publishedAt, and emit QuestionPublishedEvent")
    void shouldPublishQuestionWithValidTypeSpecificData() {
        String sourceId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(1001L, 2002L, sourceId, QuestionType.MCQ,
            "Title", "Content", 5);

        aggregate.setMcqData(new McqData(List.of(new McqOption("A", "A", true, 1.0)), false, false, false, null));
        aggregate.markEventsAsCommitted();
        Instant before = aggregate.getUpdatedAt();

        aggregate.publish();

        assertThat(aggregate.isPublished()).isTrue();
        assertThat(aggregate.getPublishedAt()).isNotNull();
        assertThat(aggregate.getUpdatedAt()).isAfter(before);
        assertThat(aggregate.getUncommittedEvents()).hasSize(1);
        assertThat(aggregate.getUncommittedEvents().get(0))
            .isInstanceOf(com.quizfun.questionbank.domain.events.QuestionPublishedEvent.class);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateBehaviorMethodsTest.Should prevent publishing without valid type data")
    @Description("publish() should throw when no valid type-specific data present")
    void shouldPreventPublishingWithoutValidTypeData() {
        String sourceId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(1001L, 2002L, sourceId, QuestionType.MCQ,
            "Title", "Content", 5);

        assertThatThrownBy(aggregate::publish)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("valid type-specific data");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateBehaviorMethodsTest.Should archive question and emit event")
    @Description("archive() should set status archived, set archivedAt, and emit QuestionArchivedEvent")
    void shouldArchiveQuestionAndEmitEvent() {
        String sourceId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(1001L, 2002L, sourceId, QuestionType.ESSAY,
            "Title", "Content", 5);

        aggregate.setEssayData(new EssayData(100, 500, true, null));
        aggregate.publish();
        aggregate.markEventsAsCommitted();

        aggregate.archive();

        assertThat(aggregate.isArchived()).isTrue();
        assertThat(aggregate.getArchivedAt()).isNotNull();
        assertThat(aggregate.getUncommittedEvents()).hasSize(1);
        assertThat(aggregate.getUncommittedEvents().get(0))
            .isInstanceOf(com.quizfun.questionbank.domain.events.QuestionArchivedEvent.class);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateBehaviorMethodsTest.Should report lifecycle states and ownership")
    @Description("isDraft/isPublished/isArchived and belonging checks should work")
    void shouldReportLifecycleStatesAndOwnership() {
        String sourceId = UUIDv7Generator.generateAsString();
        var aggregate = QuestionAggregate.createNew(111L, 222L, sourceId, QuestionType.TRUE_FALSE,
            "Title", "Content", 1);

        assertThat(aggregate.isDraft()).isTrue();
        aggregate.setTrueFalseData(new TrueFalseData(true, null, null));
        aggregate.publish();
        assertThat(aggregate.isPublished()).isTrue();
        aggregate.archive();
        assertThat(aggregate.isArchived()).isTrue();

        assertThat(aggregate.belongsToUser(111L)).isTrue();
        assertThat(aggregate.belongsToUser(112L)).isFalse();
        assertThat(aggregate.belongsToQuestionBank(222L)).isTrue();
        assertThat(aggregate.belongsToQuestionBank(223L)).isFalse();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-002.question-domain-aggregate-implementation")
    @DisplayName("QuestionAggregateBehaviorMethodsTest.Should implement business identity equals/hashCode")
    @Description("Aggregates are equal when userId, questionBankId, and sourceQuestionId match")
    void shouldImplementBusinessIdentityEqualsHashCode() {
        String sourceId = UUIDv7Generator.generateAsString();

        var a1 = QuestionAggregate.createNew(10L, 20L, sourceId, QuestionType.MCQ,
            "T1", "C1", 1);
        var a2 = QuestionAggregate.createNew(10L, 20L, sourceId, QuestionType.ESSAY,
            "T2", "C2", 2);

        // Different ids and types but same business identity
        assertThat(a1).isEqualTo(a2);
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());

        var a3 = QuestionAggregate.createNew(10L, 21L, sourceId, QuestionType.MCQ,
            "T3", "C3", 3);
        assertThat(a1).isNotEqualTo(a3);
    }
}


