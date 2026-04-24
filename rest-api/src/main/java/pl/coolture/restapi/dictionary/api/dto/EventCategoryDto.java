package pl.coolture.restapi.dictionary.api.dto;

import java.util.UUID;

/**
 * Response DTO for a single event category
 */
public record EventCategoryDto(UUID id, String name) {}