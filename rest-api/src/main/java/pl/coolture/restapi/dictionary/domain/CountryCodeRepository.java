package pl.coolture.restapi.dictionary.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * existsById(code) should be used by EventLocation validation
 */
public interface CountryCodeRepository extends JpaRepository<CountryCode, String> {}