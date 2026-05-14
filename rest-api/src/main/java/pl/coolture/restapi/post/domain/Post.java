package pl.coolture.restapi.post.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.coolture.restapi.dictionary.domain.EventCategory;
import pl.coolture.restapi.user.domain.User;

@Entity
@Table(name = "posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_category_id", nullable = false)
    private EventCategory category;

    /**
     * Null for ONLINE events. cascade = ALL so the location is persisted
     * together with the post; we treat it as owned by the post.
     *
     * Cascade ALL provides the same lifecycle to event_location
     * as happens to the post it is connected to.
     * When new Post is saved with new EventLocation
     * then EventLocation also is persisted in DB.
     *
     * Post owns its location row.
     * Each post creates its own event_locations row on insert,
     * and that row's lifetime is tied to the post's.
     *
     * No two posts will ever point at the same row
     * because nothing in the create/update flow ever reuses an id
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "event_location_id")
    private EventLocation location;

    @Column(nullable = false, length = 32)
    private String title;

    @Column(nullable = false, length = 1024)
    private String description;

    @Column(name = "event_url")
    private String eventUrl;

    @Column(nullable = false)
    private Instant startsAt;

    private Instant endsAt;

    /** Native PostgreSQL varchar[] */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "varchar(32)[]")
    private String[] tags;

    // Denormalized counters — maintained by Reactions / Participation / Comments modules.
    @Column(nullable = false)
    private int positiveReactionCount;
    @Column(nullable = false)
    private int negativeReactionCount;
    @Column(nullable = false)
    private int participantCount;
    @Column(nullable = false)
    private int commentsCount;

    /** OFFLINE | ONLINE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostType type;

    /** ACTIVE | EDITED | DELETED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostStatus status;

    /** PUBLIC | PRIVATE | FRIENDS */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PostVisibility visibility;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastModifiedAt;
    private Instant deletedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostMedia> media = new ArrayList<>();
}