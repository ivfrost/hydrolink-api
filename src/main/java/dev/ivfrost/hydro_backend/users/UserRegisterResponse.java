package dev.ivfrost.hydro_backend.users;

import java.util.List;

public record UserRegisterResponse(
    List<String> recoveryCodes) {

}
