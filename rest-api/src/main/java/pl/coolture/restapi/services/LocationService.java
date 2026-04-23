package pl.coolture.restapi.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.coolture.restapi.dtos.Post.EventLocationDTO;
import pl.coolture.restapi.models.Location;
import pl.coolture.restapi.repositories.LocationRepository;

@Service
@RequiredArgsConstructor
public class LocationService {
  private final LocationRepository locationRepository;

  public EventLocationDTO createLocation(EventLocationDTO dto) {
    Location location =
        Location.builder().nameOfVenue(dto.getNameOfVenue()).address(dto.getAddress()).build();
    locationRepository.save(location);
    return EventLocationDTO.fromEntity(location);
  }
}
