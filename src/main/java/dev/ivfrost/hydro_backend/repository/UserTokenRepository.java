package dev.ivfrost.hydro_backend.repository;

import dev.ivfrost.hydro_backend.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByTokenAndType(String token, UserToken.TokenType type);
}
