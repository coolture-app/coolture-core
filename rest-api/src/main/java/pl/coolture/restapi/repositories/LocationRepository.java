package pl.coolture.restapi.repositories;


import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.coolture.restapi.models.Location;

public interface LocationRepository extends JpaRepository<Location, UUID> {}
