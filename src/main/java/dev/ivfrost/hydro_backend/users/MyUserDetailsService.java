package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class MyUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<User> userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      throw new UsernameNotFoundException("User not found: " + username);
    }
    return new MyUserDetails(userOpt.get());
  }
}
