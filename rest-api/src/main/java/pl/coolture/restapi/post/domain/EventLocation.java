package pl.coolture.restapi.post.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "event_locations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ISO 3166-1 alpha-3. FK to country_codes.code. */
    @Column(nullable = false, length = 3)
    private String countryCode;

    @Column(length = 64)
    private String venueName;

    @Column(length = 16)
    private String buildingNum;

    @Column(length = 128)
    private String street;

    @Column(nullable = false, length = 16)
    private String postalCode;

    @Column(nullable = false, length = 128)
    private String city;

    /**
     * PostGIS geography(Point, 4326). SRID 4326 is WGS84 (standard lat/lng in degrees).
     * geography (not geometry) type accounts for earth curvature,
     * so ST_DWithin distance is in meters.
     *
     * Note: in JTS Point, x = longitude, y = latitude
     */
    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point coordinates;
}