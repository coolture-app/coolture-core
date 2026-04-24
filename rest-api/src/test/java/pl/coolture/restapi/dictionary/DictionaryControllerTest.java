package pl.coolture.restapi.dictionary;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.dictionary.api.DictionaryController;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.dictionary.application.DictionaryService;
import pl.coolture.restapi.user.application.UserProvisioningService;

@ControllerTestWithSecurity(DictionaryController.class)
class DictionaryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DictionaryService dictionaryService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final String EVENT_CATEGORIES = "/dicts/event-categories";
    private static final String COUNTRY_CODES    = "/dicts/country-codes";

    @Test
    void getEventCategories_returns200_withoutAuth() throws Exception {
        when(dictionaryService.findAllCategories()).thenReturn(List.of());

        mockMvc.perform(get(EVENT_CATEGORIES))
                .andExpect(status().isOk());
    }

    @Test
    void getCountryCodes_returns200_withoutAuth() throws Exception {
        when(dictionaryService.findAllCountryCodes()).thenReturn(List.of());

        mockMvc.perform(get(COUNTRY_CODES))
                .andExpect(status().isOk());
    }

    @Test
    void getEventCategories_returnsCategories() throws Exception {
        var id = UUID.randomUUID();
        String categoryName = "categoryName";
        when(dictionaryService.findAllCategories())
                .thenReturn(List.of(new EventCategoryDto(id, categoryName)));

        mockMvc.perform(get(EVENT_CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value(categoryName));
    }

    @Test
    void getCountryCodes_returnsCodes() throws Exception {
        String code1 = "POL";
        String code2 = "ENG";

        when(dictionaryService.findAllCountryCodes())
                .thenReturn(List.of(new CountryCodeDto(code1), new CountryCodeDto(code2)));

        mockMvc.perform(get(COUNTRY_CODES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value(code1))
                .andExpect(jsonPath("$[1].code").value(code2));
    }

    @Test
    void getEventCategories_returnsEmptyArray_whenNoneExist() throws Exception {
        when(dictionaryService.findAllCategories()).thenReturn(List.of());

        mockMvc.perform(get(EVENT_CATEGORIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEventCategories_returns200_withValidJwt() throws Exception {
        when(dictionaryService.findAllCategories()).thenReturn(List.of());

        mockMvc.perform(get(EVENT_CATEGORIES).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void getCountryCodes_returns200_withValidJwt() throws Exception {
        when(dictionaryService.findAllCategories()).thenReturn(List.of());

        mockMvc.perform(get(COUNTRY_CODES).with(jwt()))
                .andExpect(status().isOk());
    }
}