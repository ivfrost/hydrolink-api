package dev.ivfrost.hydro_backend.users;

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
    if (sub == null) {
      throw new IllegalStateException("sub in the ID JWT token cannot be null");
    }
    AuthenticatedUser principal = userResolutionService.resolveAuthenticatedUser(sub);

    return new UsernamePasswordAuthenticationToken(principal, jwt, principal.roles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList());
  }

}
