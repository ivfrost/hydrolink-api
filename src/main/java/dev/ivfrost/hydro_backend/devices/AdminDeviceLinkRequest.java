package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin link request. Role-gated, so the target user's identity id is supplied
 * explicitly: an admin has no token for the user they link on behalf of.
 */
public record AdminDeviceLinkRequest(
    @Schema(
        description = "The device secret used as ownership proof",
        example = "bc3e9dbdf08d73e760f00249da44dd68")
    @NotBlank
    @Size(max = 32)
    String secret,

    @Schema(
        description = "Cognito Identity Pool identity id of the target user, whose IoT policy will be attached",
        example = "eu-west-1:6d056010-a9f1-cbca-3607-3ed1bd952f14")
    @NotBlank
    String cognitoId
) {
}
