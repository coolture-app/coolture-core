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

    @Id
    @Column(length = 3, nullable = false)
    private String code;

    public CountryCode(String code) {
        this.code = code;
    }
}