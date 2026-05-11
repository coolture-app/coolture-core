package pl.coolture.restapi.reaction.domain;

import jakarta.persistence.*;
import java.time.Instant;

import lombok.*;

@Entity
@Table(name = "post_reactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReaction {

    @EmbeddedId
    private PostReactionId id;

    /** matches reaction_type enum in DB schema. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReactionType type;

    @Column(nullable = false)
    private Instant createdAt;
}