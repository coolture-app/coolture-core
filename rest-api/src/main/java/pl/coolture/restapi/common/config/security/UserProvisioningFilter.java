package pl.coolture.restapi.common.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.coolture.restapi.user.application.UserProvisioningService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProvisioningFilter extends OncePerRequestFilter {

    private final UserProvisioningService userProvisioningService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            try {
                userProvisioningService.ensureExists(jwtAuth.getToken());
            } catch (DataIntegrityViolationException e) {
                // TODO: ADD CACHE LATER TO MINIMALIZE THIS KIND OF SITUATIONS

                // Concurrent requests during first frontend calls (many calls in split second)
                // provisioned the same user between findById and INSERT here
                // Other transaction won and here DataIngegrityViolationException is thrown
                // Because unique primary key is being INSERTED twice the same
                // Retry in a fresh transaction - the user now exists, so the second
                // call will take the syncFromJwt path in ensureExists()
                log.debug("Concurrent user provisioning detected for JWT sub={}, retrying",
                        jwtAuth.getToken().getSubject());
                userProvisioningService.ensureExists(jwtAuth.getToken());
            }
        }

        filterChain.doFilter(request, response);
    }
}