package pl.coolture.restapi.dictionary.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventCategoryRequest(
        @NotBlank
        @Size(max = 32)
        String name
) {}