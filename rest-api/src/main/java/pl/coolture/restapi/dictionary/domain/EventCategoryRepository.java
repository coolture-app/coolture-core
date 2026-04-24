package pl.coolture.restapi.dictionary.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * findAll() from JpaRepository covers the only
 * endpoint this dictionary exposes.
 */
public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {}