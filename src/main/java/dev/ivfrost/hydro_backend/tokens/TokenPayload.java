package dev.ivfrost.hydro_backend.tokens;

import java.util.List;
import java.util.UUID;

public record TokenPayload(String username, String email, List<String> roles,
                           UUID userId) {

}
