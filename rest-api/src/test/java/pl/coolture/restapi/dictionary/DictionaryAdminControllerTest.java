package pl.coolture.restapi.dictionary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.dictionary.api.DictionaryAdminController;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeDto;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.dictionary.application.DictionaryAdminService;
import pl.coolture.restapi.dictionary.api.dto.CountryCodeRequest;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryRequest;
import pl.coolture.restapi.user.application.UserProvisioningService;

/**
 * Verifies that the admin dictionary endpoints are properly locked down.
 *
 * Before the fix these paths matched the /dicts/** permitAll rule and were
 * reachable anonymously — the scrapper exploited that to seed its fallback
 * category. The seeder now authenticates as coolture-bot with
 * ROLE_COOLTURE_ADMIN; if any of these tests start failing the bot's role
 * grant in realm.json has regressed.
 */
@ControllerTestWithSecurity(DictionaryAdminController.class)
class DictionaryAdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DictionaryAdminService adminService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final String CATEGORIES = "/dicts/admin/event-categories";
    private static final String COUNTRY_CODES = "/dicts/admin/country-codes";

    private static final SimpleGrantedAuthority ADMIN =
            new SimpleGrantedAuthority("ROLE_COOLTURE_ADMIN");
    private static final SimpleGrantedAuthority USER =
            new SimpleGrantedAuthority("ROLE_COOLTURE_USER");

    @Test
    void createCategory_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(post(CATEGORIES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"concert\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategory_returns403_whenAuthenticatedWithoutAdmin() throws Exception {
        mockMvc.perform(post(CATEGORIES)
                        .with(jwt().authorities(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"concert\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_returns201_withAdminRole() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminService.createCategory(any(EventCategoryRequest.class)))
                .thenReturn(new EventCategoryDto(id, "concert"));

        mockMvc.perform(post(CATEGORIES)
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"concert\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("concert"));
    }

    @Test
    void createCountryCode_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(post(COUNTRY_CODES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"POL\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCountryCode_returns403_whenAuthenticatedWithoutAdmin() throws Exception {
        mockMvc.perform(post(COUNTRY_CODES)
                        .with(jwt().authorities(USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"POL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCountryCode_returns201_withAdminRole() throws Exception {
        when(adminService.createCountryCode(any(CountryCodeRequest.class)))
                .thenReturn(new CountryCodeDto("POL"));

        mockMvc.perform(post(COUNTRY_CODES)
                        .with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"POL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("POL"));
    }
}
