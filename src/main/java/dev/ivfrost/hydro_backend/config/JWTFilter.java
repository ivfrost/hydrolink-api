package dev.ivfrost.hydro_backend.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@AllArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

  private final UserDetailsService userDetailsService;
  private final JWTUtil jwtUtil;
  private final ApplicationEventPublisher events;
  private final Environment environment;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    log.trace("JWTFilter processing path: {}", path);

    // Bypass filter for authentication and validation endpoints
    if (EndpointRegistry.isPublicEndpoint(path, environment)) {
      log.debug("Bypassing JWTFilter for public path: {}", path);
      filterChain.doFilter(request, response);
      return;
    }

    // Extract Authorization header
    String authHeader = request.getHeader("Authorization");
    log.trace("Authorization header: {}", authHeader);
    // Check for Bearer token
    if (authHeader != null && !authHeader.isBlank() && authHeader.startsWith("Bearer ")) {
      // Extract JWT token
      String jwt = authHeader.substring(7);
      log.trace("Extracted JWT token (trimmed length = {}): {}", jwt.length(),
          jwt.isBlank() ? "<blank>" : "<present>");
      if (jwt.isBlank()) {
        log.warn("JWT is blank or null");
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Invalid JWT Token in Bearer Header");
        return;
      }
      try {
        // Validate token and retrieve claims
        Map<String, Claim> claims = jwtUtil.validateTokenAndRetrieveClaims(jwt);
        if (claims == null) {
          log.warn("JWT contains no claims");
          throw new JWTVerificationException("No claims present");
        }

        log.trace("JWT claims keys: {}", claims.keySet());
        Long userId = claims.get("userId").asLong();
        if (userId == null) {
          log.warn("JWT contains no userId");
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JWT Token");
          return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(userId));
        log.trace("Loaded userDetails for id {}. Authorities: {}", userId,
            userDetails.getAuthorities());

        if (!userDetails.isEnabled()) {
          log.info("Rejected authentication for disabled/deleted user id: {}", userId);
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
          return;
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities());

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
          SecurityContextHolder.getContext().setAuthentication(authToken);
          log.debug("Authentication set in security context for userId: {}", userId);
        }

      } catch (JWTVerificationException e) {
        log.warn("JWT verification failed: {}", e.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                ApiResponse.error(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token").toJson())
            .getBody());
        out.flush();
        return;
      } catch (Exception e) {
        log.error("Unexpected error in JWTFilter", e);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        return;
      }
    } else {
      log.trace("No Bearer token found in Authorization header");
    }

    filterChain.doFilter(request, response);
  }

}
