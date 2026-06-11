package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class MyUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  // Despite this being a contract meant for username-based authentication, we will use the
  // user ID as the "username" for JWT-based authentication.
  @Override
  public @NonNull UserDetails loadUserByUsername(@NonNull String idString) {
    Long id = Long.parseLong(idString);

    User user = userRepository.findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));

    return new MyUserDetails(user);
  }

}
