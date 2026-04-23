package pl.coolture.restapi.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    private final CursorCodec codec = new CursorCodec();

    @Nested
    class Encode {

        @Test
        void producesNonBlankUrlSafeString() {
            var payload = new CursorPayload(UUID.randomUUID(), Instant.now());
            String cursor = codec.encode(payload);

            assertThat(cursor).isNotBlank();
            // Base64Url must not contain standard Base64 padding or unsafe chars
            assertThat(cursor).doesNotContain("+", "/", "=");
        }

        @Test
        void twoDistinctPayloadsProduceDifferentCursors() {
            var a = new CursorPayload(UUID.randomUUID(), Instant.now());
            var b = new CursorPayload(UUID.randomUUID(), Instant.now().plusSeconds(1));

            assertThat(codec.encode(a)).isNotEqualTo(codec.encode(b));
        }
    }

    @Nested
    class Decode {

        @Test
        void roundTripPreservesIdAndTimestamp() {
            // Truncate to microseconds, round-trip may lose precision
            Instant ts = Instant.now().truncatedTo(ChronoUnit.MICROS);
            UUID id = UUID.randomUUID();
            var original = new CursorPayload(id, ts);

            Optional<CursorPayload> decoded = codec.decode(codec.encode(original));

            assertThat(decoded).isPresent();
            assertThat(decoded.get().id()).isEqualTo(id);
            assertThat(decoded.get().createdAt()).isEqualTo(ts);
        }

        @Test
        void nullCursorReturnsEmpty() {
            assertThat(codec.decode(null)).isEmpty();
        }

        @Test
        void blankCursorReturnsEmpty() {
            assertThat(codec.decode("   ")).isEmpty();
        }

        @Test
        void malformedCursorReturnsEmptyInsteadOfThrowing() {
            assertThat(codec.decode("not-valid-base64!!!")).isEmpty();
        }

        @Test
        void validBase64ButWrongJsonReturnsEmpty() {
            // Valid Base64Url, but decodes to invalid CursorPayload JSON
            String garbage = java.util.Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString("{blah:123:something}".getBytes());

            assertThat(codec.decode(garbage)).isEmpty();
        }
    }
}