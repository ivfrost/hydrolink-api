package dev.ivfrost.hydro_backend.config;

import dev.ivfrost.hydro_backend.security.JWTFilter;
import dev.ivfrost.hydro_backend.service.MyUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.security.config.Customizer.withDefaults;

@AllArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MyUserDetailsService userDetailsService;
    private final JWTFilter jwtFilter;
    private final String[] allowedOrigins = {
            "https://netoasis.app",
            "87.223.194.213",
            "http://localhost:5173"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        System.out.println("Configuring security filter chain");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .addFilterBefore(this.jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(req -> req
                        .requestMatchers(
                                "/docs/",
                                "/docs/**",
                                "/v1/api/**",
                                "/v2/api-docs",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/configuration/ui",
                                "/configuration/security",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/swagger-ui.html",
                                "/v1/users",
                                "/v1/users/auth",
                                "/v1/users/password/reset",
                                "/v1/validation",
                                "/v1/validation/**",
                                "/v1/health"
                        ).permitAll()
                        .requestMatchers(
                                "/v1/me/**",
                                "/v1/users/**",
                                "/v1/devices/**"
                        ).hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .userDetailsService(this.userDetailsService)
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        System.out.println("Security filter chain configured");
        return http.build();
    }

    // Authentication manager bean to be used in AuthController
    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Password encoder bean (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS configuration
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull final CorsRegistry registry) {
                registry.addMapping("/v1/validation")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
                registry.addMapping("/v1/validation/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
                registry.addMapping("/v1/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
                registry.addMapping("/v1/users")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
                registry.addMapping("/v1/users/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
                registry.addMapping("/v1/me/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
                registry.addMapping("/v1/devices/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
                registry.addMapping("/v1/health")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*");
            }
        };
    }
}
