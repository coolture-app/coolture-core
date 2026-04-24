package pl.coolture.restapi.dictionary.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.dictionary.api.DictionaryMapper;
import pl.coolture.restapi.dictionary.domain.CountryCodeRepository;
import pl.coolture.restapi.dictionary.domain.EventCategoryRepository;

/**
 * Read-only access to dictionary data
 *
 * TODO:
 * Lists change only when an admin adds new data,
 * not on every user request. Caching them avoids repeated scans
 *
 * For the current scope a simple in-process cache would be sufficient.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DictionaryService {

    private final EventCategoryRepository categoryRepository;
    private final CountryCodeRepository countryCodeRepository;
    private final DictionaryMapper mapper;

    /** Returns all event categories ordered by name for consistent UI display. */
    // @Cacheable("event-categories")
    public List<EventCategoryDto> findAllCategories() {
        return mapper.toCategoryDtoList(
                categoryRepository.findAll(
                        org.springframework.data.domain.Sort.by("name")));
    }

    /** Returns all supported ISO 3166-1 alpha-3 country codes ordered by code. */
    // @Cacheable("country-codes")
    public List<CountryCodeDto> findAllCountryCodes() {
        return mapper.toCountryCodeDtoList(
                countryCodeRepository.findAll(
                        org.springframework.data.domain.Sort.by("code")));
    }
}