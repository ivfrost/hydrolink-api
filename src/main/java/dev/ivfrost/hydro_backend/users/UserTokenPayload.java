package dev.ivfrost.hydro_backend.users;

import java.util.List;

public record UserTokenPayload(String username, String email, List<String> roles,
                               String preferredLanguage, long userId) {

}
