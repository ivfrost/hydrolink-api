package dev.ivfrost.hydro_backend.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.ivfrost.hydro_backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JWTUtil {

    // Inject JWT secret from application properties
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.expiration-ms}")
    private Long jwtExpirationMs;
    @Value("${jwt.refresh-expiration-ms}")
    private Long jwtRefreshExpirationMs;

    // Create JWT token using the injected secret
    public String generateToken(User user) throws JWTCreationException {
        return JWT.create()
                .withSubject("User Details")
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().toString())
                .withClaim("preferredLanguage", user.getPreferredLanguage())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .withIssuedAt(new Date())
                .withIssuer("HydroBackend")
                .sign(Algorithm.HMAC256(jwtSecret));
    }

    // Create JWT refresh token using the injected secret and longer expiration
    public String generateRefreshToken(User user) throws JWTCreationException {
        return JWT.create()
                .withSubject("User Details")
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().toString())
                .withClaim("preferredLanguage", user.getPreferredLanguage())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .withIssuedAt(new Date())
                .withIssuer("HydroBackend")
                .sign(Algorithm.HMAC256(jwtSecret));
    }

    public Map<String, Claim> validateTokenAndRetrieveClaims(String token) throws JWTVerificationException {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(jwtSecret))
                .withSubject("User Details")
                .withIssuer("HydroBackend")
                .build()
                .verify(token);

        return jwt.getClaims();
    }
}