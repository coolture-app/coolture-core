package pl.coolture.restapi.reaction.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReactionType {
    LIKE("like"),
    DISLIKE("dislike");

    private final String value;

    ReactionType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static ReactionType from(String value) {
        for (ReactionType t : values()) {
            if (t.value.equals(value)) return t;
        }
        throw new IllegalArgumentException("Unknown reaction type: " + value);
    }
}