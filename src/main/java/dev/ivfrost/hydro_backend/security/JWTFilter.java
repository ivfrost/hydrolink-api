package dev.ivfrost.hydro_backend.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Component
public class JWTFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        log.info("JWTFilter processing path: {}", path);

        // Bypass filter for authentication and validation endpoints
        if (path.startsWith("/v1/users/auth/") || path.equals("/v1/users") ||
            path.equals("/v1/users/recover") || path.equals("/v1/users/password/reset") ||
            path.startsWith("/v1/validation") || path.startsWith("/v1/users/verify") ||
            path.equals("/v1/validation/recovery-code")) {

            log.info("Bypassing JWTFilter for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Extract Authorization header
        String authHeader = request.getHeader("Authorization");
        log.info("Authorization header: {}", authHeader);
        // Check for Bearer token
        if (authHeader != null && !authHeader.isBlank() && authHeader.startsWith("Bearer ")) {
            // Extract JWT token
            String jwt = authHeader.substring(7);
            log.info("Extracted JWT: {}", jwt);
            if (jwt == null || jwt.isBlank()) {
                log.warn("JWT is blank or null");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JWT Token in Bearer Header");
                return;
            }
            try {
                // Validate token and retrieve claims
                Map<String, Claim> claims = jwtUtil.validateTokenAndRetrieveClaims(jwt);
                log.info("JWT claims: {}", claims);
                String username = claims.get("username").asString();
                String role = claims.get("role") != null ? claims.get("role").asString() : null;
                log.info("JWT username: {}, role: {}", username, role);

                // Load User Details
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.info("Loaded userDetails: {}", userDetails);
                log.info("User authorities: {}", userDetails.getAuthorities());

                // Use authorities from userDetails (database), not JWT
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // Set authentication in security context
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Authentication set in security context for user: {}", username);
                } else {
                    log.info("Authentication already present in security context");
                }
            } catch (JWTVerificationException e) {
                log.error("JWT verification failed", e);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JWT Token in Bearer Header");
            } catch (Exception e) {
                log.error("Unexpected error in JWTFilter", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            }
        } else {
            log.info("No Bearer token found in Authorization header");
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
