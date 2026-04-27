package pl.coolture.restapi.dictionary.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {
    boolean existsByName(String name);
}