package pl.coolture.restapi.post.application;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ConflictException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.pagination.CursorCodec;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.CursorPayload;
import pl.coolture.restapi.post.api.PostMapper;
import pl.coolture.restapi.post.api.dto.EventLocationDto;
import pl.coolture.restapi.post.domain.EventLocation;
import pl.coolture.restapi.post.domain.EventLocationRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventLocationService {

    private final EventLocationRepository repository;
    private final PostMapper mapper;
    private final CursorCodec cursorCodec;

    public CursorPage<EventLocationDto> list(String cursor, int limit) {
        var payload = cursorCodec.decode(cursor);

        List<EventLocation> rows = repository.findPage(
                payload.map(CursorPayload::createdAt).orElse(null),
                payload.map(CursorPayload::id).orElse(null),
                limit + 1);

        List<EventLocationDto> dtos = rows.stream().map(mapper::toLocationDto).toList();
        return CursorPage.of(dtos, limit, EventLocationDto::id, EventLocationDto::createdAt, cursorCodec);
    }

    @Transactional
    public EventLocationDto create(EventLocationDto req) {
        EventLocation saved = repository.save(mapper.toLocationEntity(req));
        return mapper.toLocationDto(saved);
    }

    @Transactional
    public EventLocationDto update(UUID locationId, EventLocationDto req) {
        EventLocation loc = repository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("EventLocation", locationId));
        mapper.updateLocation(loc, req);
        return mapper.toLocationDto(loc);
    }

    @Transactional
    public void delete(UUID locationId) {
        EventLocation loc = repository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("EventLocation", locationId));
        if (repository.isReferencedByPost(locationId)) {
            throw new ConflictException("Event location is referenced by one or more posts");
        }
        repository.delete(loc);
    }
}