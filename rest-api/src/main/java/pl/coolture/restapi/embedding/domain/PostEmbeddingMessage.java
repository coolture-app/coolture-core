package pl.coolture.restapi.embedding.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostEmbeddingMessage(
    UUID postId,
    String title,
    String description,
    List<String> tags,
    Instant createdAt) {}
