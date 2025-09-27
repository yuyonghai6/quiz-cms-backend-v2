package com.quizfun.questionbank.domain.entities;

import java.util.Objects;

public class Tag {
    private final String id;
    private final String name;
    private final String color;

    public Tag(String id, String name, String color) {
        this.id = Objects.requireNonNull(id, "Tag ID cannot be null");
        this.name = Objects.requireNonNull(name, "Tag name cannot be null");
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Objects.equals(id, tag.id) &&
               Objects.equals(name, tag.name) &&
               Objects.equals(color, tag.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color);
    }

    @Override
    public String toString() {
        return "Tag{id='" + id + "', name='" + name + "', color='" + color + "'}";
    }
}