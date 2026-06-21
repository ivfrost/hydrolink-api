package dev.ivfrost.hydro_backend.config;

import static org.springframework.security.config.Customizer.withDefaults;

import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import dev.ivfrost.hydro_backend.users.MyUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Slf4j
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(SecurityConfig.CorsProperties.class)
@EnableWebSecurity
public class SecurityConfig {

  private final MyUserDetailsService userDetailsService;
  private final JWTUtil jwtUtil;
  private final ApplicationEventPublisher events;

  @ConfigurationProperties(prefix = "cors")
  public record CorsProperties(List<String> allowedOrigins) {}

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(final HttpSecurity http, Environment environment) throws Exception {
    log.info("Configuring security filter chain...");
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(HttpBasicConfigurer::disable)
        // Enable CORS
        .cors(withDefaults())
        // Disable frame options for H2 console in dev -> H2 uses an iframe
        .headers(h -> {
          if (environment.acceptsProfiles(Profiles.of("dev"))) {
            h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
          }
        })
        // Run JWTFilter in place of UsernamePasswordAuthenticationFilter
        .addFilterBefore(new JWTFilter(userDetailsService, jwtUtil, events, environment),
            UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(req -> req
            .requestMatchers(EndpointRegistry.getPublicEndpoints(environment))
            .permitAll()
            .requestMatchers(EndpointRegistry.getAuthenticatedEndpoints())
            .hasAnyRole("USER", "ADMIN")
            .anyRequest()
            .authenticated())
        .userDetailsService(this.userDetailsService)
        // Return 401 instead of redirecting to login page for unauthorized requests
        .exceptionHandling(
            e -> e.authenticationEntryPoint(
                (request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    log.info("Security filter chain configured successfully.");
    return http.build();
  }

  // Isolate device callable endpoints to a separate filter chain
  @Bean
  @Order(1)
  public SecurityFilterChain deviceSecurityFilterChain(final HttpSecurity http) {
    http.securityMatcher(EndpointRegistry.getDeviceCallableEndpoints())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Handled entirely by Controller
        .authorizeHttpRequests(req -> req.anyRequest().permitAll());

    return http.build();
  }

  // Conform to the best password encoding practices (bcrypt)
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  // Provide the CorsConfigurationSource bean referenced by http.cors(withDefaults()).
  // Allow front-end to make requests to the API.
  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

}
