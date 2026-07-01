package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import java.util.List;

public record AuthResponse(UserResponse userResponse, List<TokenResponse> tokens) {}