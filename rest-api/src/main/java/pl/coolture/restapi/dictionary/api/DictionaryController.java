package pl.coolture.restapi.dictionary.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.dictionary.application.DictionaryService;

/**
 * Read-only dictionary endpoints for client
 * to populate selectors and dropdowns of stable predefined system values
 */
@RestController
@RequestMapping("dicts")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    /**
     * Returns the full list of predefined event classifications.
     */
    @GetMapping("/event-categories")
    public ResponseEntity<List<EventCategoryDto>> getEventCategories() {
        return ResponseEntity.ok(dictionaryService.findAllCategories());
    }

    /**
     * Returns all supported ISO 3166-1 alpha-3 country codes.
     */
    @GetMapping("/country-codes")
    public ResponseEntity<List<CountryCodeDto>> getCountryCodes() {
        return ResponseEntity.ok(dictionaryService.findAllCountryCodes());
    }
}