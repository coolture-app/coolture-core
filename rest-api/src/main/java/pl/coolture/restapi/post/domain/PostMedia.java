package pl.coolture.restapi.post.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import pl.coolture.restapi.media.domain.Media;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    /** Display order within the post (0-based) */
    @Column(nullable = false)
    private int position;

    /** Only one entry per post should be true */
    @Column(name = "is_cover", nullable = false)
    private boolean isCover;
}