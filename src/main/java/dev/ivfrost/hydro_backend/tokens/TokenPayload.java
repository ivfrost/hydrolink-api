package dev.ivfrost.hydro_backend.tokens;

import java.util.List;

public record TokenPayload(String username, String email, List<String> roles,
                           long userId) {

}
