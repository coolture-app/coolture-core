package pl.coolture.restapi.post.api;

import jakarta.validation.Valid;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.post.api.dto.EventLocationDto;
import pl.coolture.restapi.post.application.EventLocationService;

// TODO: @PreAuthorize("hasRole('ADMIN')")
@RestController
@PreAuthorize("hasRole('COOLTURE_ADMIN')")
@RequestMapping("/event-locations/admin")
@RequiredArgsConstructor
public class EventLocationController {

    private final EventLocationService service;

    @GetMapping
    public CursorPage<EventLocationDto> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return service.list(cursor, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventLocationDto create(@Valid @RequestBody EventLocationDto request) {
        return service.create(request);
    }

    @PatchMapping("/{locationId}")
    public EventLocationDto update(
            @PathVariable UUID locationId,
            @Valid @RequestBody EventLocationDto request) {
        return service.update(locationId, request);
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID locationId) {
        service.delete(locationId);
    }
}