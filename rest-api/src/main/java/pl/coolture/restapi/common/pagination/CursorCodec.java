package pl.coolture.restapi.common.pagination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Encodes and decodes opaque cursor strings that are exchanged with clients
 *
 * Wire format: Base64Url( JSON { "id": "<uuid>", "createdAt": "<timestamp>" } )
 *
 * The cursor is intentionally opaque to clients bcs the internal structure
 * is an implementation detail and may change
 *
 * On decode a malformed cursor returns Optional.empty(),
 * which the caller treats the same as "first page".
 * This avoids 500 errors when a client sends corrupted cursor.
 */
@Slf4j
@Component
public class CursorCodec {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /**
     * Encodes a CursorPayload into the opaque string returned in API responses.
     *
     * @throws IllegalStateException if serialisation fails - only can happen when payload is bad
     */
    public String encode(CursorPayload payload) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(payload);
            return ENCODER.encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode cursor payload", e);
        }
    }

    /**
     * Decodes a cursor string received from a client.
     *
     * Returns Optional.empty() when the cursor is null or cannot be decoded.
     * The caller should then start from the beginning of the feed.
     */
    public Optional<CursorPayload> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return Optional.empty();
        }

        try {
            byte[] json = DECODER.decode(cursor);
            return Optional.of(MAPPER.readValue(json, CursorPayload.class));
        } catch (Exception e) {
            // do not throw server exception, bad cursor is not a server error
            log.debug("Could not decode cursor '{}': {}", cursor, e.getMessage());
            return Optional.empty();
        }
    }
}