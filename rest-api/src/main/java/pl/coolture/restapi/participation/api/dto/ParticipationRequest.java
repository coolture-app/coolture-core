package pl.coolture.restapi.participation.api.dto;

import jakarta.validation.constraints.NotNull;
import pl.coolture.restapi.participation.domain.ParticipationType;

public record ParticipationRequest(@NotNull ParticipationType type) {}