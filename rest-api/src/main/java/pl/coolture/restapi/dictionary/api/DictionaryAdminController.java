package pl.coolture.restapi.dictionary.api;

import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeRequest;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryRequest;
import pl.coolture.restapi.dictionary.application.DictionaryAdminService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Admin only dictionary management endpoints
 */
@RestController
@PreAuthorize("hasRole('COOLTURE_ADMIN')")
@RequestMapping("/dicts/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COOLTURE_ADMIN')")
public class DictionaryAdminController {

    private final DictionaryAdminService adminService;

    @PostMapping("/event-categories")
    public ResponseEntity<EventCategoryDto> createCategory(
            @RequestBody @Valid EventCategoryRequest request) {
        return ResponseEntity.status(201).body(adminService.createCategory(request));
    }

    @PutMapping("/event-categories/{id}")
    public ResponseEntity<EventCategoryDto> updateCategory(
            @PathVariable UUID id,
            @RequestBody @Valid EventCategoryRequest request) {
        return ResponseEntity.ok(adminService.updateCategory(id, request));
    }

    @DeleteMapping("/event-categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        adminService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/country-codes")
    public ResponseEntity<CountryCodeDto> createCountryCode(
            @RequestBody @Valid CountryCodeRequest request) {
        return ResponseEntity.status(201).body(adminService.createCountryCode(request));
    }

    /**
     * Country codes are natural PKs so "update" replaces the old code
     * with a new one (delete + insert).
     */
    @PutMapping("/country-codes/{code}")
    public ResponseEntity<CountryCodeDto> updateCountryCode(
            @PathVariable String code,
            @RequestBody @Valid CountryCodeRequest request) {
        return ResponseEntity.ok(adminService.updateCountryCode(code, request));
    }

    @DeleteMapping("/country-codes/{code}")
    public ResponseEntity<Void> deleteCountryCode(@PathVariable String code) {
        adminService.deleteCountryCode(code);
        return ResponseEntity.noContent().build();
    }
}