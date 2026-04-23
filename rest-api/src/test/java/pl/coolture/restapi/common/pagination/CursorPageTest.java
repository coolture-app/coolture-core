package pl.coolture.restapi.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;

class CursorPageTest {

    // minimal entity mock
    record Item(UUID id, Instant createdAt) {}

    private final CursorCodec codec = new CursorCodec();

    private List<Item> items(int count) {
        List<Item> list = new ArrayList<>();

        String instantString = "2024-01-01T00:00:00Z";
        Instant baseInstant = Instant.parse(instantString);

        for (int i = 0; i < count; i++) {
            list.add(new Item(UUID.randomUUID(), baseInstant.plusSeconds(i)));
        }
        return list;
    }

    @Test
    void exactlyLimitRows_hasMoreFalse_noCursor() {
        int limit = 10;
        // repo returns exactly `limit` rows
        List<Item> rows = items(limit);
        CursorPage<Item> page = CursorPage.of(rows, limit, Item::id, Item::createdAt, codec);

        assertThat(page.items()).hasSize(limit);
        assertThat(page.page().isHasMore()).isFalse();
        assertThat(page.page().getNextCursor()).isNull();
    }

    @Test
    void limitPlusOneRows_hasMoreTrue_cursorPointsAtLastVisibleItem() {
        int limit = 5;
        // we ask for this one more item over `limit` to determine if `hasMore`
        List<Item> rows = items(limit + 1);
        CursorPage<Item> page = CursorPage.of(rows, limit, Item::id, Item::createdAt, codec);

        assertThat(page.items()).hasSize(limit);
        assertThat(page.page().isHasMore()).isTrue();
        assertThat(page.page().getNextCursor()).isNotNull();

        // Cursor must decode to the last item that was actually returned
        // remember that this +1 is truncated after `hasMore` check
        Item expectedLast = rows.get(limit - 1);
        Optional<CursorPayload> cursor = codec.decode(page.page().getNextCursor());
        assertThat(cursor).isPresent();
        assertThat(cursor.get().id()).isEqualTo(expectedLast.id());
        assertThat(cursor.get().createdAt()).isEqualTo(expectedLast.createdAt());
    }

    @Test
    void emptyRows_hasMoreFalse_emptyItemsList() {
        CursorPage<Item> page = CursorPage.of(List.of(), 20, Item::id, Item::createdAt, codec);

        assertThat(page.items()).isEmpty();
        assertThat(page.page().isHasMore()).isFalse();
        assertThat(page.page().getNextCursor()).isNull();
    }

    @Test
    void metaLimitReflectsRequestedLimit() {
        int requestedLimit = 20;
        CursorPage<Item> page = CursorPage.of(items(3), requestedLimit, Item::id, Item::createdAt, codec);

        assertThat(page.page().getLimit()).isEqualTo(requestedLimit);
    }

    @Test
    void probeRowIsNotIncludedInItems() {
        int limit = 3;
        List<Item> rows = items(limit + 1);
        Item probeRow = rows.getLast();

        CursorPage<Item> page = CursorPage.of(rows, limit, Item::id, Item::createdAt, codec);

        assertThat(page.items()).doesNotContain(probeRow);
    }
}
