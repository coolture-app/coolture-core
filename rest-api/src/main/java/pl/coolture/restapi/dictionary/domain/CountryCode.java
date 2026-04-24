package pl.coolture.restapi.dictionary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Country code lookup
 * The code itself is the natural primary key no surrogate UUID needed
 */
@Entity
@Table(name = "country_codes")
@Getter
@NoArgsConstructor
public class CountryCode {

    /**
     * ISO 3166-1 alpha-3 code, e.g. "POL", "DEU", "USA".
     * Length is fixed at 3 characters per the standard.
     */
    @Id
    @Column(length = 3, nullable = false)
    private String code;
}