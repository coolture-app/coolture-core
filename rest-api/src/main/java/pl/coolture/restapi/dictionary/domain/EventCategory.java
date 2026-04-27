package pl.coolture.restapi.dictionary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Predefined event classification loaded from the database.
 * Categories are managed by admins,
 * so no CRUD endpoints are exposed to users
 */
@Entity
@Table(name = "event_categories")
@Getter
@NoArgsConstructor
public class EventCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String name;

    public EventCategory(String name) {
        this.name = name;
    }

    /** Used by admin update - keeps the same UUID. */
    public void rename(String newName) {
        this.name = newName;
    }
}