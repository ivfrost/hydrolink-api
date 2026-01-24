package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.User;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
public class MyUserDetails implements UserDetails {

  private final User user;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (user == null || user.getRoles() == null) {
      return List.of();
    }
    return user.getRoles().stream()
        .map(role -> {
          String rn = role.name();
          return new SimpleGrantedAuthority(rn.startsWith("ROLE_") ? rn : "ROLE_" + rn);
        })
        .toList();
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return String.valueOf(user.getId());
  }

  @Override
  public boolean isEnabled() {
    return user.isEnabled();
  }

}
