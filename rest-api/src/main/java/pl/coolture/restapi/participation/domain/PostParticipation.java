package pl.coolture.restapi.participation.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "post_participations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostParticipation {

    @EmbeddedId
    private PostParticipationId id;

    /** matches participation_type enum in DB schema. */
    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false)
    private Instant createdAt;
}