package dev.ivfrost.hydro_backend.config;

import static org.springframework.security.config.Customizer.withDefaults;

import dev.ivfrost.hydro_backend.users.MyUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@EnableWebSecurity
public class SecurityConfig {

  private final MyUserDetailsService userDetailsService;
  private final JWTFilter jwtFilter;
  @Value("${cors.allowed-origins}")
  private String[] allowedOrigins;

  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    log.info("Configuring security filter chain...");
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(HttpBasicConfigurer::disable)
        // Enable CORS
        .cors(withDefaults())
        // Disable frame options for H2 console in dev
        .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        // Run JWTFilter in place of UsernamePasswordAuthenticationFilter
//        .addFilterBefore(authRequestCountFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(req -> req
            .requestMatchers(EndpointRegistry.H2_CONSOLE.toArray(new String[0]))
            .permitAll()
            .requestMatchers(EndpointRegistry.SWAGGER.toArray(new String[0]))
            .permitAll()
            .requestMatchers(EndpointRegistry.APP_PUBLIC.toArray(new String[0]))
            .permitAll()
            .requestMatchers(EndpointRegistry.APP_AUTHENTICATED.toArray(new String[0]))
            .hasAnyRole("USER", "ADMIN")
            .anyRequest()
            .authenticated())
        .userDetailsService(this.userDetailsService)
        .exceptionHandling(
            e -> e.authenticationEntryPoint(
                (request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    log.info("Security filter chain configured successfully.");
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
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

}
