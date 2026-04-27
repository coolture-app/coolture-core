package pl.coolture.restapi.reaction.api.dto;

import jakarta.validation.constraints.NotNull;
import pl.coolture.restapi.reaction.domain.ReactionType;

public record ReactionRequest(@NotNull ReactionType type) {}
