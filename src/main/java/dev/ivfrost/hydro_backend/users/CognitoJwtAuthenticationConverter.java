package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.User;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>{

  private final UserResolutionService userResolutionService;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String sub = jwt.getSubject();
    User user = userResolutionService.resolveUser(sub);

    Set<String> roles = user.getRoles().stream()
        .map(userRole -> userRole.getRole().name())
        .collect(Collectors.toUnmodifiableSet());

    AuthenticatedUser principal = new AuthenticatedUser(user.getId(), sub, user.getUsername(), roles);

    return new UsernamePasswordAuthenticationToken(principal, jwt, roles.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList());
  }

}
