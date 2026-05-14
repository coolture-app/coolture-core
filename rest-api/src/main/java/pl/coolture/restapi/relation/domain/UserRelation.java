package pl.coolture.restapi.relation.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.coolture.restapi.user.domain.User;

@Entity
@Table(name = "user_relations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRelation {

    @EmbeddedId
    private UserRelationId id;

    /**
     * FOLLOW - source follows target.
     * BLOCK  - source blocks target. Replaces any existing FOLLOW in the same direction.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRelationType type;

    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sourceUserId")
    @JoinColumn(name = "source_user_id")
    private User sourceUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("targetUserId")
    @JoinColumn(name = "target_user_id")
    private User targetUser;
}