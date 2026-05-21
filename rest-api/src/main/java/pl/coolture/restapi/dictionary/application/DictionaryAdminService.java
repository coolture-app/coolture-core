package pl.coolture.restapi.dictionary.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ConflictException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.dictionary.api.DictionaryMapper;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeRequest;
import pl.coolture.restapi.dictionary.domain.CountryCode;
import pl.coolture.restapi.dictionary.domain.CountryCodeRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class DictionaryAdminService {

    private final CountryCodeRepository countryCodeRepository;
    private final DictionaryMapper mapper;

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