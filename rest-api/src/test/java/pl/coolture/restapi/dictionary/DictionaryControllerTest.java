package pl.coolture.restapi.dictionary;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.dictionary.api.DictionaryController;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.application.DictionaryService;
import pl.coolture.restapi.user.application.UserProvisioningService;

@ControllerTestWithSecurity(DictionaryController.class)
class DictionaryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DictionaryService dictionaryService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final String COUNTRY_CODES = "/dicts/country-codes";

    @Test
    void getCountryCodes_returns200_withoutAuth() throws Exception {
        when(dictionaryService.findAllCountryCodes()).thenReturn(List.of());

        mockMvc.perform(get(COUNTRY_CODES))
                .andExpect(status().isOk());
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
    void getCountryCodes_returns200_withValidJwt() throws Exception {
        when(dictionaryService.findAllCountryCodes()).thenReturn(List.of());

        mockMvc.perform(get(COUNTRY_CODES).with(jwt()))
                .andExpect(status().isOk());
    }
}