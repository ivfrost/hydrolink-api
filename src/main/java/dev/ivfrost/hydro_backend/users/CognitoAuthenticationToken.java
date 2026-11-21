package dev.ivfrost.hydro_backend.users;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Authentication carrying the {@link AuthenticatedUser} principal and the Cognito ID token as
 * credentials.
 *
 * <p>Not {@code UsernamePasswordAuthenticationToken}: {@code ProviderManager} erases the
 * credentials of the token its provider returns, and that implementation nulls them, which would
 * leave callers of {@code getCredentials()} without the token that Cognito Identity Pool login
 * needs.
 */
public class CognitoAuthenticationToken extends AbstractAuthenticationToken {

  private final AuthenticatedUser principal;
  private final Jwt credentials;

  public CognitoAuthenticationToken(AuthenticatedUser principal, Jwt credentials,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.credentials = credentials;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }
}
