package pl.coolture.restapi.comment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.coolture.restapi.post.domain.Post;
import pl.coolture.restapi.user.domain.User;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Top-level comment of the thread. Null when this it is itself a root comment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_comment_id")
    private Comment rootComment;

    /** Direct parent. Null for root comments. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    /**
     * Materialized path - chain of ancestor IDs from root down to (but excluding)
     * this comment. Length equals comment depth: 0=root, 1=reply, 2=reply-to-reply.
     * Stored as native PostgreSQL uuid[] for easy sort and building trees without recursion.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ancestor_ids", columnDefinition = "uuid[]")
    private UUID[] ancestorIds;

    @Column(nullable = false, length = 512)
    private String content;

    /** Denormalized counter of direct replies. Maintained by CommentService. */
    @Column(nullable = false)
    private int repliesCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Set on the first edit. Null while the comment is still in original state. */
    private Instant lastEditedAt;

    private Instant deletedAt;

    /** ACTIVE | DELETED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CommentStatus status;
}