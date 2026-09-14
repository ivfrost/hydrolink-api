package dev.ivfrost.hydro_backend.users;

import java.util.Set;
import java.util.UUID;

/**
 * Authenticated principal exposed to the rest of the application.
 *
 * <p>This is a deliberately small, stable view of the signed-in user. The JWT-to-principal converter
 * builds it, and controllers or services that need the current identity read from it, instead of the
 * internal {@code User} entity leaking out of the {@code users} module as the principal type.
 *
 * @param id       the application user id (the DB row key, a UUID)
 * @param sub      the Cognito subject the token was bound to
 * @param username the application username
 * @param roles    the application roles, without the {@code ROLE_} prefix
 */
public record AuthenticatedUser(
    UUID id,
    String sub,
    String username,
    Set<String> roles
) {
}
