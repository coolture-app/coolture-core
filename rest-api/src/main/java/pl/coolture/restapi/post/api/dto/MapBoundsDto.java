package pl.coolture.restapi.post.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.awt.*;

@Data
public class MapBoundsDto {
    @Valid
    @NotNull
    private GeoPointDto leftUpper;

    @Valid
    @NotNull
    private GeoPointDto rightBottom;
}
