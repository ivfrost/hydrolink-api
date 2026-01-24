package dev.ivfrost.hydro_backend.config;

import dev.ivfrost.hydro_backend.devices.DeviceLoadEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class AuthRequestCountFilter extends OncePerRequestFilter {

  private final ApplicationEventPublisher events;
  private int count = 0;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      log.trace("No authentication found in security context.");
    } else if (authentication.isAuthenticated()) {
      count++;
      log.trace("Authenticated request count: {} (principal={})", count, authentication.getName());
      // Publish only when we have a numeric user id as principal
      String principalName = authentication.getName();
      if (principalName != null) {
        try {
          long userId = Long.parseLong(principalName);
          if (count == 1) {
            events.publishEvent(new DeviceLoadEvent(userId));
          }
        } catch (NumberFormatException ex) {
          log.debug("Skipping DeviceLoadEvent: principal '{}' is not a numeric user id.",
              principalName);
        }
      }
    }
    filterChain.doFilter(request, response);
  }
}
