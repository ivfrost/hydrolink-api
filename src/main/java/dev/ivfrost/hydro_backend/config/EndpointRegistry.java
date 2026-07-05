package dev.ivfrost.hydro_backend.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.AntPathMatcher;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EndpointRegistry {

  private static final List<String> APP_PUBLIC = List.of(
      "/v1/users",
      "/v1/users/auth",
      "/v1/users/recover",
      "/v1/users/validate/**",
      "/v1/users/password/reset",
      "/v1/validation/**",
      "/v1/internal/mqtt/**",
      "/actuator/**"
  );
  private static final List<String> SWAGGER = List.of(
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/api-docs",
      "/api-docs/**",
      "/api-docs-json",
      "/api-docs-json/**"
  );
  private static final List<String> APP_AUTHENTICATED = List.of(
      "/v1/users/**",
      "/v1/me/**",
      "/v1/users/auth/refresh"
  );
  private static final List<String> H2_CONSOLE = List.of(
      "/h2-console/**",
      "/h2-console"
  );
  private static final List<String> DEVICE_CALLABLE = List.of(
      "/v1/internal/devices/auth",
      "/v1/internal/devices/last-seen",
      "/v1/internal/devices/provision"
  );

  private static final AntPathMatcher pathMatcher = new AntPathMatcher();

  public static String[] getPublicEndpoints(Environment env) {
    Stream<List<String>> base = Stream.of(APP_PUBLIC);

    if (env.acceptsProfiles(Profiles.of("dev", "stage"))) {
      base = Stream.concat(base, Stream.of(SWAGGER));
    }

    if (env.acceptsProfiles(Profiles.of("dev"))) {
      base = Stream.concat(base, Stream.of(H2_CONSOLE));
    }

    return base.flatMap(List::stream).toArray(String[]::new);
  }

  public static boolean isPublicEndpoint(String path, Environment env) {
    return Arrays.stream(getPublicEndpoints(env))
        .anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  public static String[] getAuthenticatedEndpoints() {
    return APP_AUTHENTICATED.toArray(new String[0]);
  }

  public static String[] getDeviceCallableEndpoints() {
    return DEVICE_CALLABLE.toArray(new String[0]);
  }
}
