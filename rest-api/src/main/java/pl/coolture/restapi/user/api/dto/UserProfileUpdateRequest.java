package pl.coolture.restapi.user.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for PATCH that relates to user profile
 * All fields are optional, only non-null values are applied to the entity
 * That's how MapStruct is configured right now
 */
public record UserProfileUpdateRequest(
        @Size(max = 32)  String username,
        @Size(max = 128) String firstName,
        @Size(max = 128) String lastName,
        @Size(max = 512) String bio) {}