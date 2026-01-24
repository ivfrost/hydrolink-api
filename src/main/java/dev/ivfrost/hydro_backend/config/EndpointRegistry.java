package dev.ivfrost.hydro_backend.config;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.util.AntPathMatcher;


public class EndpointRegistry {

  static final List<String> APP_PUBLIC = List.of(
      "/v1/users",
      "/v1/users/auth",
      "/v1/users/recover",
      "/v1/users/password/reset",
      "/v1/validation/**",
      "/v1/health"
  );
  static final List<String> SWAGGER = List.of(
      "/v3/api-docs",
      "/v3/api-docs/**",
      "/swagger-ui.html",
      "/swagger-ui/**"
  );
  static final List<String> APP_AUTHENTICATED = List.of(
      "/v1/users/**",
      "/v1/me/**",
      "/v1/users/auth/refresh"
  );
  static final List<String> H2_CONSOLE = List.of(
      "/h2-console/**",
      "/h2-console"
  );
  private static final AntPathMatcher pathMatcher = new AntPathMatcher();
  private static final List<String> PUBLIC_ENDPOINTS;

  static {
    PUBLIC_ENDPOINTS = Stream.of(APP_PUBLIC, SWAGGER, H2_CONSOLE)
        .flatMap(List::stream)
        .toList();
  }

  private EndpointRegistry() {

  }

  public static boolean isPublicEndpoint(String path) {
    return PUBLIC_ENDPOINTS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
  }
}
