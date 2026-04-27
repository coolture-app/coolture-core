package pl.coolture.restapi.dictionary.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ConflictException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.dictionary.api.DictionaryMapper;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeRequest;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryRequest;
import pl.coolture.restapi.dictionary.domain.CountryCode;
import pl.coolture.restapi.dictionary.domain.CountryCodeRepository;
import pl.coolture.restapi.dictionary.domain.EventCategory;
import pl.coolture.restapi.dictionary.domain.EventCategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class DictionaryAdminService {

    private final EventCategoryRepository categoryRepository;
    private final CountryCodeRepository countryCodeRepository;
    private final DictionaryMapper mapper;

    public EventCategoryDto createCategory(EventCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException("Category already exists: " + request.name());
        }
        return mapper.toDto(categoryRepository.save(new EventCategory(request.name())));
    }

    public EventCategoryDto updateCategory(UUID id, EventCategoryRequest request) {
        EventCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.rename(request.name());
        return mapper.toDto(category); // saved on tx commit
    }

    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }

    public CountryCodeDto createCountryCode(CountryCodeRequest request) {
        if (countryCodeRepository.existsById(request.code())) {
            throw new ConflictException("Country code already exists: " + request.code());
        }
        return mapper.toDto(countryCodeRepository.save(new CountryCode(request.code())));
    }

    /**
     * Because the code is the PK, renaming it requires a delete + insert.
     */
    public CountryCodeDto updateCountryCode(String oldCode, CountryCodeRequest request) {
        if (!countryCodeRepository.existsById(oldCode)) {
            throw new ResourceNotFoundException("Country code not found: " + oldCode);
        }
        if (!oldCode.equals(request.code()) && countryCodeRepository.existsById(request.code())) {
            throw new ConflictException("Country code already exists: " + request.code());
        }
        countryCodeRepository.deleteById(oldCode);
        return mapper.toDto(countryCodeRepository.save(new CountryCode(request.code())));
    }

    public void deleteCountryCode(String code) {
        if (!countryCodeRepository.existsById(code)) {
            throw new ResourceNotFoundException("Country code not found: " + code);
        }
        countryCodeRepository.deleteById(code);
    }
}