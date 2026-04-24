package pl.coolture.restapi.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import pl.coolture.restapi.common.config.security.SecurityConfig;

/**
 * annotation for controller unit tests that need simulation of security
 * Bundles real SecurityConfig and TestSecurityConfig,
 * removes need to repeat the @Import boilerplate.
 *
 * Usage:
 *   @ControllerTest(DictionaryController.class)
 *   class DictionaryControllerTest { ... }
 *
 * The test class still needs:
 *   @MockitoBean UserProvisioningService userProvisioningService;
 * because UserProvisioningFilter is a @Component inside SecurityConfig's filter chain.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WebMvcTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class, TestSecurityConfig.class})
public @interface ControllerTestWithSecurity {

    @AliasFor(annotation = WebMvcTest.class, attribute = "value")
    Class<?>[] value() default {};
}