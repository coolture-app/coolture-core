package pl.coolture.restapi.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.dtos.Post.EventLocationDTO;
import pl.coolture.restapi.services.LocationService;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {
  private final LocationService locationService;

  @PostMapping
  public ResponseEntity<EventLocationDTO> createLocation(@RequestBody @Valid EventLocationDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(locationService.createLocation(dto));
  }
}
