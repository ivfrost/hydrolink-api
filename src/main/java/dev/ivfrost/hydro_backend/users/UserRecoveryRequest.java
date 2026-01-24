package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.tokens.RecoveryCodeUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserRecoveryRequest(@Email(message = "Invalid email format") String email, @Size(
    min = RecoveryCodeUtil.RECOVERY_CODE_LENGTH,
    max = RecoveryCodeUtil.RECOVERY_CODE_LENGTH,
    message = "Wrong recovery code length") String recoveryCode,
                                  @Size(min = 8, max = 60, message = "New password must be between 8 and 60 characters long") String newPassword) {

}
