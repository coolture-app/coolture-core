package pl.coolture.restapi.dtos.Post;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.coolture.restapi.models.Location;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLocationDTO {
  private UUID id;

  @NotBlank(message = "Venue name cannot be blank")
  @Size(max = 255, message = "Venue name must not exceed 255 characters")
  private String nameOfVenue;

  @NotBlank(message = "Address cannot be blank")
  @Size(max = 255, message = "Address must not exceed 255 characters")
  private String address;

  public static EventLocationDTO fromEntity(Location location) {
    if (location == null) {
      return null;
    }
    return EventLocationDTO.builder()
        .id(location.getUuid())
        .nameOfVenue(location.getNameOfVenue())
        .address(location.getAddress())
        .build();
  }
}
